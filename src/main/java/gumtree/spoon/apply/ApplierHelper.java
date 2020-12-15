package gumtree.spoon.apply;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.actions.MyAction.MyDelete;
import com.github.gumtreediff.actions.MyAction.MyInsert;
import com.github.gumtreediff.actions.MyAction.MyMove;
import com.github.gumtreediff.actions.MyAction.MyUpdate;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionedTree;

import org.apache.commons.compress.utils.Iterators;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jetty.util.MultiMap;

import gumtree.spoon.apply.ActionApplier;
import gumtree.spoon.apply.Combination;
import gumtree.spoon.apply.WrongAstContextException;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import gumtree.spoon.diff.operations.Operation;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.support.DefaultOutputDestinationHandler;
import spoon.support.JavaOutputProcessor;
import spoon.support.OutputDestinationHandler;
import spoon.support.StandardEnvironment;

public abstract class ApplierHelper<T> implements AutoCloseable {
    Logger logger = Logger.getLogger(ApplierHelper.class.getName());

    protected SpoonGumTreeBuilder scanner;
    protected AbstractVersionedTree middle;
    protected Diff diff;
    protected List<MyAction<AbstractVersionedTree>> actions;
    public final EvoStateMaintainer evoState;
    protected Consumer<Set<T>> validityLauncher;

    public void setValidityLauncher(Consumer<Set<T>> validityLauncher) {
        this.validityLauncher = validityLauncher;
    }

    protected float meanSimConst = .99f;
    protected float minReqConst = .9f;
    public final Map<T, Set<T>> evoToEvo;
    protected Factory factory;
    protected Launcher launcher;
    protected MultiDiffImpl mdiff;
    protected int leafsActionsLimit;

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

    protected class EvoStateMaintainer {
        protected Map<T, Integer> evoState = new HashMap<>();
        protected Map<T, Integer> evoReqSize = new HashMap<>();
        // protected Map<T, Set<MyAction<AbstractVersionedTree>>> evoToTree = new HashMap<>();
        protected final Map<T, Set<T>> evoToEvo = ApplierHelper.this.evoToEvo;
        protected Map<AtomicAction<AbstractVersionedTree>, Set<T>> presentMap = new HashMap<>();
        // protected Map<Object, Set<T>> absentMap = new HashMap<>();
        protected Map<MyAction<AbstractVersionedTree>, Boolean> reqState = new HashMap<>(); // false by default
        protected Set<T> validable = new HashSet<>();

        protected EvoStateMaintainer() {
            for (T e : evoToEvo.keySet()) {
                markRequirements(new Chain<>(e));
            }
            for (Set<T> values : presentMap.values()) {
                for (T evolution : values) {
                    evoReqSize.put(evolution, evoReqSize.getOrDefault(evolution, 0) + 1);
                }
            }
        }

        protected boolean set(MyAction<AbstractVersionedTree> a, boolean inverted, boolean silent) {
            Boolean prev = reqState.put(a, inverted);
            if (prev != null && prev == inverted) {
                // nothing to do
                return false;
            } else {
                for (T e : presentMap.getOrDefault(a, Collections.emptySet())) {
                    Integer v = evoState.getOrDefault(e, 0);
                    evoState.put(e, inverted ? v - 1 : v + 1);
                    if (isLaunchable(e)) {
                        validable.add(e);
                    } else {
                        validable.remove(e);
                    }
                }
                if (!silent) {
                    triggerCallback();
                }
                return true;
            }
        }

        protected void triggerCallback() {
            validityLauncher.accept(Collections.unmodifiableSet(new HashSet<>(validable)));
        }

        private boolean isLaunchable(T e) {
            return ratio(e).floatValue() > minReqConst;
        }

        public Fraction ratio(T e) {
            return Fraction.getFraction(evoState.get(e), evoReqSize.get(e));
        }

        public Set<T> getEvolutions(Set<T> e) {
            Set<T> r = new HashSet<>();
            getEvolutions(e, r);
            return r;
        }

        private void getEvolutions(Set<T> evos, Set<T> r) {
            for (T e : evos) {
                r.add(e);
                if (evoToEvo.containsKey(e)) {
                    getEvolutions(evoToEvo.get(e), r);
                }
            }
        }

        public Set<MyAction<AbstractVersionedTree>> getActions(Set<T> e) {
            Set<MyAction<AbstractVersionedTree>> r = new HashSet<>();
            for (T e1 : e) {
                Object original = getOriginal(e1);
                if (original instanceof Operation) {
                    MyAction<AbstractVersionedTree> action = (MyAction<AbstractVersionedTree>) ((Operation) original)
                            .getAction();
                    r.add(action);
                }
            }
            return r;
        }

        public boolean isCurrentlyApplied(MyAction<AbstractVersionedTree> a) {
            // if (a instanceof Move) {
            // Boolean s = reqState.get(a.getSource());
            // Boolean t = reqState.get(a.getTarget());
            // return s && t;
            // } else {
            // return reqState.get(a.getTarget());
            // }
            return reqState.get(a);
        }

        public Set<T> getComponents(T e) {
            return Collections.unmodifiableSet(evoToEvo.get(e));
        }

        private void markRequirements(Chain<T> evos) {
            Object original = getOriginal(evos.curr);
            if (original instanceof AtomicAction) {
                markRequirements((AtomicAction<AbstractVersionedTree>) original, evos);
            }
            Set<T> compo = evoToEvo.get(evos.curr);
            if (compo != null) {
                for (T e : compo) {
                    if(!evos.contains(e)){
                        Chain<T> newe = new Chain<>(e, evos);
                        markRequirements(newe);
                    }
                }
            }
        }

        private void coarseDecompose(Set<MyAction<?>> r, MyAction<?> a) {
            if (a instanceof AtomicAction) {
                r.add(a);
            } else {
                for (MyAction<?> ca : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                    r.add(ca);
                    coarseDecompose(r, ca);
                }
            }
        }

        private void markRequirements(AtomicAction<AbstractVersionedTree> a, Collection<T> e) {
            presentMap.putIfAbsent(a, new HashSet<>());
            for (T ee : e) {
                Object original = getOriginal(ee);
                if (a == original) {
                    // needed to refere to atomic action in the db
                    presentMap.get(a).add(ee);
                } else if (original instanceof AtomicAction) {
                    // here we do nothing, to keep AtomicActions precise
                } else if (original instanceof ComposedAction) {
                    // here we only point from atomic actions exactly part of the composed one
                    Set<MyAction<?>> decomposed = new HashSet<>();
                    coarseDecompose(decomposed, (ComposedAction<AbstractVersionedTree>)original);
                    if (decomposed.contains(a)) {
                        presentMap.get(a).add(ee);
                    }
                } else {
                    // other source of evolutions
                    presentMap.get(a).add(ee);
                }
            }
            // if (a instanceof Delete) {
            // // markDeleteRequirements(a, e);
            // // } else if (a instanceof Move) {
            // // markSourceRequirements(a, e);
            // // markInsertRequirements(a, e);
            // } else {
            // markInsertRequirements(a, e);
            // // markContextRequirements(a, e);
            // }
        }

        private void markInsertRequirements(MyAction<AbstractVersionedTree> a, Collection<T> e) {
            // AbstractVersionedTree t = a.getTarget();
            // assert (t != null);
            // // AbstractVersionedTree p = t.getParent();
            // presentMap.putIfAbsent(t, new HashSet<>());
            // presentMap.get(t).addAll(e);
        }

        private void markDeleteRequirements(MyAction<AbstractVersionedTree> a, Collection<T> e) {
            // AbstractVersionedTree t = a.getTarget();
            // assert (t != null);
            // absentMap.putIfAbsent(t, new HashSet<>());
            // absentMap.get(t).addAll(e);
            // for (AbstractVersionedTree c : t.getAllChildren()) {
            // absentMap.putIfAbsent(c, new HashSet<>());
            // absentMap.get(c).addAll(e);
            // }
        }

        // /**
        // * For a move, the insertion must be done before the deletion.
        // * Thus the inserted node must be populated before the removed node is
        // unpopulated.
        // * @param a
        // * @param e
        // */
        // private void markSourceRequirements(MyMove a, Collection<T> e)
        // {
        // AbstractVersionedTree s = (AbstractVersionedTree)a.getSource();
        // AbstractVersionedTree t = a.getTarget();
        // assert (t != null);
        // assert (s != null);
        // presentMap.putIfAbsent(s, new HashSet<>());
        // presentMap.get(s).addAll(e);
        // }

        // private void markContextRequirements(MyAction<AbstractVersionedTree><?> a, Collection<T> e) {
        // // TODO
        // }
    }

    protected ApplierHelper(SpoonGumTreeBuilder scanner, AbstractVersionedTree middle, Diff diff,
            Map<T, Set<T>> atomizedRefactorings) {
        this.scanner = scanner;
        this.middle = middle;
        this.factory = (Factory) middle.getMetadata("Factory");
        this.launcher = (Launcher) middle.getMetadata("Launcher");
        this.diff = diff;
        this.actions = (List) ((DiffImpl) diff).getAtomic();
        this.evoToEvo = atomizedRefactorings;
        this.evoState = new EvoStateMaintainer();
    }

    // public ApplierHelper(EvolutionsAtProj eap, Map<T, Set<T>> atomizedRefactorings) {
    //     this(eap.getScanner(), eap.getMdiff().getMiddle(), eap.getDiff(), atomizedRefactorings);
    //     this.mdiff = eap.getMdiff();
    // }

    public void setLeafsActionsLimit(int limit) {
        this.leafsActionsLimit = limit;
    }

    public Launcher applyEvolutions(Set<T> wantedEvos) {
        Set<MyAction<?>> actions = new HashSet<>();
        for (T evo : wantedEvos) {
            Object ori = getOriginal(evo);
            if (ori instanceof MyAction) {
                actions.add((MyAction)ori);
            } else {
                Set<T> components = new HashSet<>();
                Set<T> tmp = evoToEvo.get(evo);
                if (tmp != null) {
                    components.addAll(tmp);
                }
                ComposedAction<AbstractVersionedTree> composed = new VirtComposedAction(components);
                if (composed.composed().size()>0) {
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
        if (internalIsInTest(a)) {
            testSet.add(a);
        } else if (internalIsInApp(a)) {
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
        extractActions(wantedEvos, acts);
        return acts;

    }

    private void extractActions(Set<T> wantedEvos, Set<MyAction<AbstractVersionedTree>> acts) {
        for (T evolution : wantedEvos) {
            Object original = getOriginal(evolution);
            if (original instanceof MyAction) {
                acts.add((MyAction<AbstractVersionedTree>) /* ((Operation) */ original/* ).getAction() */);
            }
            Set<T> others = evoState.evoToEvo.get(evolution);
            if (others != null) {
                extractActions(others, acts);
            }
        }
    }

    protected abstract Object getOriginal(T evolution);

    public Launcher applyAllActions() {
        return applyCombActions(actions);
    }

    private ComposedAction<AbstractVersionedTree> getCAction(AbstractVersionedTree tree, boolean way) {
        return (ComposedAction<AbstractVersionedTree>) tree.getMetadata("VirtuallyComposed");
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
        Flattener2 flat = Combination.flatten(wantedActions);
        flat.compute();
        for (MyAction<?> ca : wantedActions) {
            if (ca instanceof ComposedAction) {
                flat.clusterize((ComposedAction<AbstractVersionedTree>)ca);
            }
        }
        Set<Cluster2> toBreak = new HashSet<>();
        LinkedList<Cluster2> tryToBreak = new LinkedList<>();
        List<ImmutablePair<Integer, Cluster2>> constrainedTree = flat.getConstrainedTree(toBreak);
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
        tryToBreak.removeIf(x->x.nodes.size()<=1);
        tryToBreak.sort((a,b)->b.nodes.size()-a.nodes.size());
        while (j < 10) {
            Set<Cluster2> tmp = new HashSet<>();
            tmp.addAll(toBreak);
            if (tryToBreak.isEmpty()) {
                break;
            }
            tmp.add(tryToBreak.poll());
            List<ImmutablePair<Integer, Cluster2>> tmp2 = flat.getConstrainedTree(tmp);
            int c = 0;
            for (int x : Combination.detectLeafs(constrainedTree)) {
                c += x == 0 ? 0 : 1;
            }
            if (c <= leafsActionsLimit && c > prevSize) {
                prevSize = c;
                toBreak = tmp;
                constrainedTree = tmp2;
                tryToBreak.clear();
                for (ImmutablePair<Integer,Cluster2> pair : tmp2) {
                    tryToBreak.add(pair.right);
                }
                tryToBreak.removeIf(x->x.nodes.size()<=1);
                tryToBreak.sort((a,b)->b.nodes.size()-a.nodes.size());
            }
            j++;
        }
        Combination.CombinationHelper<Cluster2> combs = Combination.build(flat, constrainedTree);
        logger.info("On track for at least 2^"+combs.minExposant()+" cases");
        // int exp = combs.minExposant();
        // if (exp > leafsActionsLimit) {
        //     logger.warning(exp + " leafs would make too much combinations");
        //     StringBuilder infoBuilder = new StringBuilder();
        //     for (AbstractVersionedTree t : combs.getLeafList()) {
        //         infoBuilder.append("\n -");
        //         Object a = t.getMetadata("VirtuallyComposed");
        //         if (a != null) {
        //             infoBuilder.append(a.toString());
        //         } else {
        //             a = t.getMetadata(MyScriptGenerator.INSERT_ACTION);
        //             if (a != null) {
        //                 infoBuilder.append(a.toString());
        //             } else {
        //                 a = t.getMetadata(MyScriptGenerator.DELETE_ACTION);
        //                 infoBuilder.append(a.toString());
        //             }
        //         }
        //     }
        //     logger.info("Following set of actions would need too much combinations" + infoBuilder);
        //     return null;
        // }
        Map<AbstractVersionedTree, Boolean> waitingToBeApplied = new HashMap<>();
        do {
            Combination.CHANGE<Cluster2> change = combs.next();
            boolean b = false;
            boolean clustDefPres = flat.isInitiallyPresent(change.content.root);
            // boolean clustDefPres = change.content.initiallyPresentNodes.contains(change.content.root);
            for (AbstractVersionedTree n : change.content.nodes) {
                boolean nDefPres = flat.isInitiallyPresent(n);
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
                            } catch (gumtree.spoon.apply.WrongAstContextException e) {
                            }
                        }
                    } while (waitingHasbeApplied);
                    // // ComposedAction<AbstractVersionedTree> caction = getCAction(change.content, change.way);
                    // if (caction != null) {
                    //     for (MyAction<?> myAction : caction.composed()) {
                    //         b |= applyCombActionsAux(waitingToBeApplied, change.way, myAction);
                    //     }
                    // } else {
                    //     AtomicAction<AbstractVersionedTree> aaction = getAAction(change.content, change.way);
                    //     boolean inverted = aaction == null;
                    //     aaction = inverted ? getAAction(change.content, !change.way) : aaction;
                    //     b |= auxApply(scanner, this.factory, aaction, inverted);
                    //     for (AbstractVersionedTree node : waitingToBeApplied.keySet()) {
                    //         try {
                    //             AtomicAction<AbstractVersionedTree> action2 = getAAction(node,
                    //                     waitingToBeApplied.get(node));
                    //             boolean inverted2 = action2 == null;
                    //             action2 = inverted2 ? getAAction(node, !waitingToBeApplied.get(node)) : action2;
                    //             auxApply(scanner, this.factory, action2, inverted2);
                    //             waitingToBeApplied.remove(node);
                    //         } catch (gumtree.spoon.apply.WrongAstContextException e) {
                    //         }
                    //     }
                    // }
                    if (b)
                        evoState.triggerCallback();
                } catch (gumtree.spoon.apply.WrongAstContextException e) {
                    waitingToBeApplied.put(n, change.way);
                }
            }
        } while (!combs.isInit());
        return launcher;
    }

    // private boolean applyCombActionsAux(Map<AbstractVersionedTree, Boolean> waitingToBeApplied, boolean way,
    //         MyAction<?> myAction2) throws WrongAstContextException {
    //     boolean b = false;
    //     if (myAction2 instanceof ComposedAction) {
    //         for (MyAction<?> myAction : ((ComposedAction<AbstractVersionedTree>) myAction2).composed()) {
    //             b |= applyCombActionsAux(waitingToBeApplied, way, myAction);
    //         }
    //     } else if (myAction2 instanceof AtomicAction) {
    //         AtomicAction<AbstractVersionedTree> action = (AtomicAction<AbstractVersionedTree>) myAction2;
    //         AtomicAction<AbstractVersionedTree> aaction = getAAction(action.getTarget(), way);
    //         boolean inverted = aaction == null;
    //         aaction = inverted ? getAAction(action.getTarget(), !way) : aaction;
    //         b |= auxApply(scanner, this.factory, aaction, inverted);
    //         for (AbstractVersionedTree node : waitingToBeApplied.keySet()) {
    //             try {
    //                 AtomicAction<AbstractVersionedTree> action2 = getAAction(node, waitingToBeApplied.get(node));
    //                 boolean inverted2 = action2 == null;
    //                 action2 = inverted2 ? getAAction(node, !waitingToBeApplied.get(node)) : action2;
    //                 auxApply(scanner, this.factory, action2, inverted2);
    //                 waitingToBeApplied.remove(node);
    //             } catch (gumtree.spoon.apply.WrongAstContextException e) {
    //             }
    //         }
    //     }
    //     return b;
    // }

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
            boolean inverted) throws WrongAstContextException {
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

    protected abstract Boolean isInApp(CtElement element);

    protected abstract Boolean isInTest(CtElement element);

    private boolean internalIsInApp(MyAction<?> a) {
        if (a instanceof AtomicAction) {
            AbstractVersionedTree tree = ((AtomicAction<AbstractVersionedTree>) a).getTarget();
            assert tree != null;
            while (tree != null) {
                CtElement ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
                if (ele == null) {
                    tree = tree.getParent();
                    continue;
                }
                Boolean tmp = isInApp(ele);
                if (tmp == null) {
                    tree = tree.getParent();
                    continue;
                }
                return tmp;
                // if (ele instanceof CtReference) {
                //     tree = tree.getParent();
                //     continue;
                // }
                // SourcePosition pos = ele.getPosition();
                // if (pos == null) {
                //     tree = tree.getParent();
                //     continue;
                // }
                // CompilationUnit cu = pos.getCompilationUnit();
                // if (cu == null) {
                //     tree = tree.getParent();
                //     continue;
                // }
                // Boolean isTest = (Boolean) cu.getMetadata("isTest");
                // if (isTest != null && isTest) {
                //     return false;
                // } else {
                //     return true;
                // }
            }
        } else if (a instanceof ComposedAction) {
            for (MyAction<?> aa : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                if (!internalIsInApp(aa)) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean internalIsInTest(MyAction<?> a) {
        if (a instanceof AtomicAction) {
            AbstractVersionedTree tree = ((AtomicAction<AbstractVersionedTree>) a).getTarget();
            assert tree != null;
            while (tree != null) {
                CtElement ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
                if (ele == null) {
                    tree = tree.getParent();
                    continue;
                }
                Boolean tmp = isInTest(ele);
                if (tmp == null) {
                    tree = tree.getParent();
                    continue;
                }
                return tmp;
                // if (ele instanceof CtReference) {
                //     tree = tree.getParent();
                //     continue;
                // }
                // SourcePosition pos = ele.getPosition();
                // if (pos == null) {
                //     tree = tree.getParent();
                //     continue;
                // }
                // CompilationUnit cu = pos.getCompilationUnit();
                // if (cu == null) {
                //     tree = tree.getParent();
                //     continue;
                // }
                // Boolean isTest = (Boolean) cu.getMetadata("isTest");
                // if (isTest != null && isTest) {
                //     return true;
                // } else {
                //     return false;
                // }
            }
        } else if (a instanceof ComposedAction) {
            for (MyAction<?> aa : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                if (!internalIsInTest(aa)) {
                    return false;
                }
            }
        }
        return false;
    }
}
