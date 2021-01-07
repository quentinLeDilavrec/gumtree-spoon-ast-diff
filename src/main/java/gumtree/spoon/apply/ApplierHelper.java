package gumtree.spoon.apply;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.actions.MyAction.MyDelete;
import com.github.gumtreediff.actions.MyAction.MyInsert;
import com.github.gumtreediff.actions.MyAction.MyMove;
import com.github.gumtreediff.actions.MyAction.MyUpdate;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionedTree;

import org.apache.commons.lang3.tuple.ImmutablePair;

import gumtree.spoon.apply.EvoStateMaintainer.Section;
import gumtree.spoon.apply.Flattener.ComposingClusterizer;
import gumtree.spoon.apply.Flattener.Clusterizer.Cluster;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import spoon.Launcher;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;

public class ApplierHelper<T> implements AutoCloseable {
    Logger logger = Logger.getLogger(ApplierHelper.class.getName());

    protected SpoonGumTreeBuilder scanner;
    protected AbstractVersionedTree middle;
    protected Diff diff;
    protected List<MyAction<AbstractVersionedTree>> actions;
    public final EvoStateMaintainer<T> evoState;

    // protected float meanSimConst = .99f;
    // protected float minReqConst = .9f;
    // public final Map<T, Set<T>> evoToEvo;
    protected Factory factory;
    protected Launcher launcher;
    protected MultiDiffImpl mdiff;
    protected int leafsActionsLimit;

    public ApplierHelper(SpoonGumTreeBuilder scanner, AbstractVersionedTree middle, Diff diff,
            EvoStateMaintainer<T> evoStateMaintainer) {
        this.scanner = scanner;
        this.middle = middle;
        this.factory = (Factory) middle.getMetadata("Factory");
        this.launcher = (Launcher) middle.getMetadata("Launcher");
        this.diff = diff;
        this.actions = (List) ((DiffImpl) diff).getAtomic();
        this.evoState = evoStateMaintainer;
    }

    public void setLeafsActionsLimit(int limit) {
        this.leafsActionsLimit = limit;
    }

    public Launcher applyEvolutions(Set<T> wantedEvos) {
        Set<MyAction<?>> actions = new HashSet<>();
        for (T evo : wantedEvos) {
            Object ori = evoState.getOriginal(evo);
            if (ori instanceof MyAction) {
                actions.add((MyAction) ori);
            } else {
                Set<T> components = new HashSet<>();
                Set<T> tmp = evoState.evoToEvo.get(evo);
                if (tmp != null) {
                    components.addAll(tmp);
                }
                ComposedAction<AbstractVersionedTree> composed = new VirtComposedAction(components);
                if (composed.composed().size() > 0) {
                    actions.add(composed);
                }
            }
        }
        Collection<MyAction<?>> testSet = new HashSet<>();
        Collection<MyAction<?>> appSet = new HashSet<>();
        for (MyAction<?> a : actions) {
            popTopGroups(testSet, appSet, a);
        }

        actions.add(new ComposedAction<AbstractVersionedTree>() {
            private ArrayList<MyAction<?>> components = new ArrayList<>(testSet);

            @Override
            public String getName() {
                return "Virt Group of Tests";
            }

            @Override
            public List<? extends MyAction<?>> composed() {
                return Collections.unmodifiableList(components);
            }
        });

        actions.add(new ComposedAction<AbstractVersionedTree>() {
            private ArrayList<MyAction<?>> components = new ArrayList<>(appSet);

            @Override
            public String getName() {
                return "Virt Group of App";
            }

            @Override
            public List<? extends MyAction<?>> composed() {
                return Collections.unmodifiableList(components);
            }
        });
        return applyCombActions(actions);
    }

    private void popTopGroups(Collection<MyAction<?>> testSet, Collection<MyAction<?>> appSet, MyAction<?> a) {
        if (isInTest(a)) {
            testSet.add(a);
        } else if (isInApp(a)) {
            appSet.add(a);
        }
        if (a instanceof ComposedAction) {
            for (MyAction<?> ca : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                popTopGroups(testSet, appSet, ca);
            }
        }
    }

    private Set<MyAction<AbstractVersionedTree>> extractActions(Set<T> wantedEvos) {
        Set<MyAction<AbstractVersionedTree>> acts = new HashSet<>();
        extractActions(wantedEvos, null, acts);
        return acts;

    }

    private void extractActions(Set<T> wantedEvos, Chain<T> path, Set<MyAction<AbstractVersionedTree>> acts) {
        for (T evolution : wantedEvos) {
            Object original = evoState.getOriginal(evolution);
            if (original instanceof MyAction) {
                acts.add((MyAction<AbstractVersionedTree>) /* ((Operation) */ original/* ).getAction() */);
            }
            Set<T> others = evoState.evoToEvo.get(evolution);
            if (others != null && (path == null || !path.contains(evolution))) {
                extractActions(others, path == null ? new Chain<>(evolution) : new Chain<>(evolution, path), acts);
            }
        }
    }

    public Launcher applyAllActions() {
        return applyCombActions(actions);
    }

    private AtomicAction<AbstractVersionedTree> getAAction(AbstractVersionedTree tree, boolean way) {
        if (way) {
            return (AtomicAction<AbstractVersionedTree>) tree.getMetadata(MyScriptGenerator.INSERT_ACTION);
        } else {
            return (AtomicAction<AbstractVersionedTree>) tree.getMetadata(MyScriptGenerator.DELETE_ACTION);
        }
    }

    private MyAction<AbstractVersionedTree> invertAction(MyAction<AbstractVersionedTree> action) {
        MyAction<AbstractVersionedTree> r;
        if (action instanceof Insert) {
            r = MyAction.invert((MyInsert) action);
        } else if (action instanceof Delete) {
            r = MyAction.invert((MyDelete) action);
        } else if (action instanceof Update) {
            r = MyAction.invert((MyUpdate) action);
        } else {
            throw new RuntimeException(action.getClass().getCanonicalName());
        }
        return r;
    }

    private Launcher applyCombActions(Collection<? extends MyAction<?>> wantedActions) {
        Set<AtomicAction<AbstractVersionedTree>> wantedAA = new HashSet<>();

        ComposingClusterizer flat = Combination.flatten(evoState.globalClusterizer, wantedActions);
        for (MyAction<?> ca : wantedActions) {
            if (ca instanceof ComposedAction) {
                flat.clusterize((ComposedAction<AbstractVersionedTree>)ca);
            }
        }
        Set<Cluster> toBreak = new HashSet<>();
        LinkedList<Cluster> tryToBreak = new LinkedList<>();
        flat.setInibiteds(toBreak);
        List<ImmutablePair<Integer, Cluster>> constrainedTree = flat.getConstrainedTree();
        int j = 0;
        int prevSize = 0;
        for (int x : Combination.detectLeafs(constrainedTree)) {
            prevSize += x == 0 ? 0 : 1;
        }
        for (MyAction<?> a : wantedActions) {
            decompose(wantedAA, a);
            if (a instanceof ComposedAction) {
                tryToBreak.addAll(flat.getCluster((ComposedAction<AbstractVersionedTree>) a));
            }
        }
        tryToBreak.removeIf(x->x.getNodes().size()<=1);
        tryToBreak.sort((a,b)->b.getNodes().size()-a.getNodes().size());
        while (j < 10) {
            Set<Cluster> tmp = new HashSet<>();
            tmp.addAll(toBreak);
            if (tryToBreak.isEmpty()) {
                break;
            }
            tmp.add(tryToBreak.poll());
            flat.setInibiteds(tmp);
            List<ImmutablePair<Integer, Cluster>> tmp2 = flat.getConstrainedTree();
            int c = 0;
            for (int x : Combination.detectLeafs(constrainedTree)) {
                c += x == 0 ? 0 : 1;
            }
            if (c <= leafsActionsLimit && c > prevSize) {
                prevSize = c;
                toBreak = tmp;
                constrainedTree = tmp2;
                tryToBreak.clear();
                for (ImmutablePair<Integer,Cluster> pair : tmp2) {
                    tryToBreak.add(pair.right);
                }
                tryToBreak.removeIf(x->x.getNodes().size()<=1);
                tryToBreak.sort((a,b)->b.getNodes().size()-a.getNodes().size());
            }
            j++;
        }
        Combination.CombinationHelper<Cluster> combs = Combination.build(flat, constrainedTree);
        logger.info("On track for at least 2^"+combs.minExposant()+" cases");
        Map<AbstractVersionedTree, Boolean> waitingToBeApplied = new HashMap<>();
        do {
            Combination.CHANGE<Cluster> change = combs.next();
            boolean b = false;
            boolean clustDefPres = flat.getActions().contains(change.content.getRoot().getMetadata(MyScriptGenerator.DELETE_ACTION));
            for (AbstractVersionedTree n : change.content.getNodes()) {
                boolean nDefPres = flat.getActions().contains(n.getMetadata(MyScriptGenerator.DELETE_ACTION));
                boolean way = clustDefPres == nDefPres ? change.way : !change.way;
                try {
                    AtomicAction<AbstractVersionedTree> aaction = getAAction(n, way);
                    boolean inverted = aaction == null;
                    aaction = inverted ? getAAction(n, !way) : aaction;
                    b |= auxApply(scanner, this.factory, aaction, wantedAA, inverted);
                    waitingToBeApplied.remove(n);
                    boolean waitingHasbeApplied;
                    do {
                        waitingHasbeApplied = false;
                        for (AbstractVersionedTree node : waitingToBeApplied.keySet()) {
                            try {
                                AtomicAction<AbstractVersionedTree> action2 = getAAction(node,
                                        waitingToBeApplied.get(node));
                                boolean inverted2 = action2 == null;
                                action2 = inverted2 ? getAAction(node, !waitingToBeApplied.get(node)) : action2;
                                auxApply(scanner, this.factory, action2, wantedAA, inverted2);
                                waitingToBeApplied.remove(node);
                                waitingHasbeApplied = true;
                            } catch (WrongAstContextException|MissingParentException e) {
                            }
                        }
                    } while (waitingHasbeApplied);
                    if (b)
                        evoState.triggerCallback();
                } catch (WrongAstContextException e) {
                    waitingToBeApplied.put(n, change.way);
                } catch (MissingParentException e) {
                    logger.log(Level.WARNING,"problem while applying atomic evolution", e);
                    waitingToBeApplied.put(n, change.way);
                }
            }
        } while (!combs.isInit());
        return launcher;
    }

    private void decompose(Set<AtomicAction<AbstractVersionedTree>> wantedAA, MyAction<?> a) {
        if (a instanceof AtomicAction) {
            wantedAA.add((AtomicAction<AbstractVersionedTree>) a);
        } else {
            for (MyAction<?> ca : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                decompose(wantedAA, ca);
            }
        }
    }

    public boolean auxApply(final SpoonGumTreeBuilder scanner, Factory facto,
            AtomicAction<AbstractVersionedTree> action, Set<AtomicAction<AbstractVersionedTree>> otherWantedAA,
            boolean inverted) throws WrongAstContextException, MissingParentException {
        MyAction<AbstractVersionedTree> invertableAction = inverted ? invertAction(action) : action;
        if (invertableAction instanceof Insert) {
            ActionApplier.applyMyInsert(facto, scanner.getTreeContext(), (MyInsert) invertableAction);
            // Object dst =
            // invertableAction.getTarget().getMetadata(MyScriptGenerator.MOVE_DST_ACTION);
            AbstractVersionedTree dst = invertableAction.getTarget();
            MyMove mact = (MyMove) dst.getMetadata(MyScriptGenerator.MOVE_SRC_ACTION);
            if (mact != null) {
                AbstractVersionedTree src = (AbstractVersionedTree) mact.getDelete().getTarget();
                if (watching.containsKey(src)) {
                    watching.put(src, dst);
                }
            }
            for (AtomicAction<AbstractVersionedTree> compressed : ((MyAction.CompressibleAtomicAction<AbstractVersionedTree>) action)
                    .getCompressed()) {
                if (!otherWantedAA.contains(compressed)) {
                    auxApply(scanner, facto, compressed, otherWantedAA, inverted);
                }
            }
        } else if (invertableAction instanceof Delete) {
            for (AtomicAction<AbstractVersionedTree> compressed : ((MyAction.CompressibleAtomicAction<AbstractVersionedTree>) action)
                    .getCompressed()) {
                if (!otherWantedAA.contains(compressed)) {
                    auxApply(scanner, facto, compressed, otherWantedAA, inverted);
                }
            }
            ActionApplier.applyMyDelete(facto, scanner.getTreeContext(), (MyDelete) invertableAction);
            // return evoState.set(invertableAction.getTarget(), false, true);
        } else if (invertableAction instanceof Update) {
            ActionApplier.applyMyUpdate(facto, scanner.getTreeContext(), (MyUpdate) invertableAction);
            AbstractVersionedTree target = invertableAction.getTarget();
            if (inverted) {
                if (watching.containsKey(target)) {
                    watching.put(target, target);
                }
            } else {
                AbstractVersionedTree src = (AbstractVersionedTree) ((MyUpdate) invertableAction).getNode();
                if (watching.containsKey(src)) {
                    watching.put(src, target);
                }
            }
            for (AtomicAction<AbstractVersionedTree> compressed : ((MyAction.CompressibleAtomicAction<AbstractVersionedTree>) action)
                    .getCompressed()) {
                if (!otherWantedAA.contains(compressed)) {
                    auxApply(scanner, facto, compressed, otherWantedAA, inverted);
                }
            }
        } else {
            throw new RuntimeException(action.getClass().getCanonicalName());
        }
        return evoState.set(action, inverted, true);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        // TODO undo all changes on middle repr as spoon ast
    }

    private Map<AbstractVersionedTree, AbstractVersionedTree> watching = new HashMap<>();

    public CtElement[] watchApply(Version version, AbstractVersionedTree... treeTest) {
        CtElement[] r = new CtElement[treeTest.length];
        for (int i = 0; i < treeTest.length; i++) {
            AbstractVersionedTree x = treeTest[i];
            watching.put(x, x);
            CtElement ele = computeAt(version, x);
            if (ele == null && x.getInsertVersion() == null) {
                ele = (CtElement) x.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
            }
            r[i] = ele;
        }
        return r;
    }

    public CtElement[] getUpdatedElement(Version version, AbstractVersionedTree... treeTest) {
        CtElement[] r = new CtElement[treeTest.length];
        for (int i = 0; i < treeTest.length; i++) {
            AbstractVersionedTree xx = treeTest[i];
            AbstractVersionedTree x = watching.getOrDefault(xx, xx);
            if (x.getInsertVersion() != version) {
                continue;
            }
            r[i] = computeAt(version, x);
        }
        return r;
    }

    public CtMethod getUpdatedMethod(Version version, AbstractVersionedTree treeTest) {
        AbstractVersionedTree xx = treeTest;
        AbstractVersionedTree xxName = treeTest.getChildren(treeTest.getInsertVersion()).get(0);
        AbstractVersionedTree x = watching.getOrDefault(xx, xx);
        AbstractVersionedTree xName = watching.getOrDefault(xxName, xxName);
        CtMethod actualTest = (CtMethod) x.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        // CtExecutableReference actualTestRef = (CtExecutableReference) xName
        //         .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        // if (actualTest != actualTestRef.getParent()) {
        //     throw new RuntimeException();
        // }
        String simpname = actualTest.getSimpleName();
        String qualDeclClass = actualTest.getDeclaringType().getQualifiedName();

        CtMethod ele = computeAt(version, x);
        if (ele == null) {
            CtExecutableReference ref = computeAt(version, xName);
            if (ref != null) {
                ele = (CtMethod) ref.getParent();
            }
        }

        if (ele != null && ele.getSimpleName().equals(simpname)
                && ele.getDeclaringType().getQualifiedName().equals(qualDeclClass)) {
        } else {
            ele = (CtMethod) x.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
        }
        return ele;
    }

    private <T> T computeAt(Version version, AbstractVersionedTree x) {
        Object r = null;
        if (x.getInsertVersion() == version) {
            r = x.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
        }
        if (r == null) {
            Map<Version, Object> map = (Map<Version, Object>) x
                    .getMetadata(MyScriptGenerator.ORIGINAL_SPOON_OBJECT_PER_VERSION);
            if (map != null) {
                r = map.get(version);
            }
        }
        return (T) r;
    }

    private boolean isInApp(MyAction<?> a) {
        if (a instanceof AtomicAction) {
            AbstractVersionedTree tree = ((AtomicAction<AbstractVersionedTree>) a).getTarget();
            assert tree != null;
            while (tree != null) {
                CtElement ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
                if (ele == null) {
                    tree = tree.getParent();
                    continue;
                }
                Boolean tmp = evoState.isInApp(ele);
                if (tmp == null) {
                    tree = tree.getParent();
                    continue;
                }
                return tmp;
            }
        } else if (a instanceof ComposedAction) {
            for (MyAction<?> aa : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                if (!isInApp(aa)) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isInTest(MyAction<?> a) {
        if (a instanceof AtomicAction) {
            AbstractVersionedTree tree = ((AtomicAction<AbstractVersionedTree>) a).getTarget();
            assert tree != null;
            while (tree != null) {
                CtElement ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
                if (ele == null) {
                    tree = tree.getParent();
                    continue;
                }
                Boolean tmp = evoState.isInTest(ele);
                if (tmp == null) {
                    tree = tree.getParent();
                    continue;
                }
                return tmp;
            }
        } else if (a instanceof ComposedAction) {
            for (MyAction<?> aa : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                if (!isInTest(aa)) {
                    return false;
                }
            }
        }
        return false;
    }
    
    // TODO is it better than isInTest/isInApp ?
    private Set<Section> sectionSpanning(MyAction<?> a) {
        if (a instanceof AtomicAction) {
            AbstractVersionedTree tree = ((AtomicAction<AbstractVersionedTree>) a).getTarget();
            assert tree != null;
            while (tree != null) {
                CtElement ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
                if (ele == null) {
                    tree = tree.getParent();
                    continue;
                }
                Set<Section> tmp = evoState.sectionSpanning(ele);
                if (tmp == null) {
                    tree = tree.getParent();
                    continue;
                }
                return tmp;
            }
        } else if (a instanceof ComposedAction) {
            Set<Section> r = new HashSet<>();
            for (MyAction<?> aa : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                r.addAll(sectionSpanning(aa));
            }
            return r;
        }
        return Collections.EMPTY_SET;
    }

    private final class VirtComposedAction implements ComposedAction<AbstractVersionedTree> {
        private final Set<T> components;
        List<MyAction<AbstractVersionedTree>> compo;

        private VirtComposedAction(Set<T> components) {
            this.compo = new ArrayList<>(extractActions(components));
            this.components = components;
        }

        @Override
        public List<MyAction<AbstractVersionedTree>> composed() {
            return compo;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String toString() {
            StringBuilder r = new StringBuilder();
            r.append("Virt composed action containing:");
            for (MyAction<AbstractVersionedTree> component : composed()) {
                r.append("\n\t-");
                r.append(component.toString());
            }
            return r.toString();
        }
    }
}
