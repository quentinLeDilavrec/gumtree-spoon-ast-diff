package gumtree.spoon.apply;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    protected class EvoStateMaintainer {
        protected Map<T, Integer> evoState = new HashMap<>();
        protected Map<T, Integer> evoReqSize = new HashMap<>();
        // protected Map<T, Set<MyAction<AbstractVersionedTree>>> evoToTree = new HashMap<>();
        protected final Map<T, Set<T>> evoToEvo = ApplierHelper.this.evoToEvo;
        protected Map<MyAction<AbstractVersionedTree>, Set<T>> presentMap = new HashMap<>();
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
            if (original instanceof Action) {
                Action action = (Action) original;// ((Operation) original).getAction();
                // evoToTree.putIfAbsent(evos.curr, new HashSet<>());
                // evoToTree.get(evos.curr).add((MyAction<AbstractVersionedTree>) action);
                markRequirements((MyAction<AbstractVersionedTree>) action, evos);
            }
            Set<T> compo = evoToEvo.get(evos.curr);
            if (compo != null) {
                for (T e : compo) {
                    Chain<T> newe = new Chain<>(e, evos);
                    markRequirements(newe);
                }
            }
        }

        private void markRequirements(MyAction<AbstractVersionedTree> a, Collection<T> e) {
            presentMap.putIfAbsent(a, new HashSet<>());
            presentMap.get(a).addAll(e);
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
        Set<MyAction<AbstractVersionedTree>> acts = new HashSet<>();
        extractActions(wantedEvos, acts);
        return applyCombActions(acts);
    }

    private void extractActions(Set<T> wantedEvos, Set<MyAction<AbstractVersionedTree>> acts) {
        for (T evolution : wantedEvos) {
            Object original = getOriginal(evolution);
            if (original instanceof Action) {
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

    private MyAction<AbstractVersionedTree> getAction(AbstractVersionedTree tree, boolean way) {
        MyAction<AbstractVersionedTree> action;
        if (way) {
            action = (MyAction<AbstractVersionedTree>) tree.getMetadata(MyScriptGenerator.INSERT_ACTION);
        } else {
            action = (MyAction<AbstractVersionedTree>) tree.getMetadata(MyScriptGenerator.DELETE_ACTION);
        }
        return action;
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

    private Launcher applyCombActions(Collection<MyAction<AbstractVersionedTree>> wantedActions) {
        Map<AbstractVersionedTree, Boolean> waitingToBeApplied = new HashMap<>();
        Combination.CombinationHelper<AbstractVersionedTree> combs = Combination.build(middle, wantedActions);
        int exp = combs.minExposant();
        if (exp > leafsActionsLimit) {
            logger.warning(exp + " leafs would make too much combinations");
            return null;
        }
        do {
            Combination.CHANGE<AbstractVersionedTree> change = combs.next();
            try {
                MyAction<AbstractVersionedTree> action = getAction(change.content, change.way);
                boolean inverted = action == null;
                action = inverted ? getAction(change.content, !change.way) : action;
                boolean b = auxApply(scanner, this.factory, action, inverted);
                for (AbstractVersionedTree node : waitingToBeApplied.keySet()) {
                    try {
                        MyAction<AbstractVersionedTree> action2 = getAction(node, waitingToBeApplied.get(node));
                        boolean inverted2 = action2 == null;
                        action2 = inverted2 ? getAction(node, !waitingToBeApplied.get(node)) : action2;
                        auxApply(scanner, this.factory, action2, inverted2);
                        waitingToBeApplied.remove(node);
                    } catch (gumtree.spoon.apply.WrongAstContextException e) {
                    }
                }
                if (b)
                    evoState.triggerCallback();
            } catch (gumtree.spoon.apply.WrongAstContextException e) {
                waitingToBeApplied.put(change.content, change.way);
            }
        } while (!combs.isInit());
        return launcher;
    }

    public boolean auxApply(final SpoonGumTreeBuilder scanner, Factory facto, MyAction<AbstractVersionedTree> action,
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
            for (MyAction<AbstractVersionedTree> compressed : ((MyInsert) action).getCompressed()) {
                auxApply(scanner, facto, compressed, inverted);
            }
        } else if (invertableAction instanceof Delete) {
            for (MyAction<AbstractVersionedTree> compressed : ((MyDelete) action).getCompressed()) {
                auxApply(scanner, facto, compressed, inverted);
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
            for (MyAction<AbstractVersionedTree> compressed : ((MyUpdate) action).getCompressed()) {
                auxApply(scanner, facto, compressed, inverted);
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

}
