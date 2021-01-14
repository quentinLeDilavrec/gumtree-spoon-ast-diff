package gumtree.spoon.apply;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.Version;

import org.apache.commons.lang3.math.Fraction;

import gumtree.spoon.apply.Flattener.Clusterizer;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.CtElement;

public abstract class EvoStateMaintainer<T> {
    public interface Section {}

    protected abstract Boolean isInApp(CtElement element);
    protected abstract Boolean isInTest(CtElement element);
    protected abstract Set<Section> sectionSpanning(CtElement ele);
    protected abstract Consumer<Set<T>> getValidityLauncher();
    protected abstract boolean isLaunchable(T e);
    public abstract Object getOriginal(T evolution);

    public final Version initialState;
    protected Map<T, Integer> evoState = new HashMap<>();
    protected Map<T, Integer> evoReqSize = new HashMap<>();
    // protected Map<T, Set<MyAction<AbstractVersionedTree>>> evoToTree = new HashMap<>();
    protected final Map<T, Set<T>> evoToEvo;
    protected Map<AtomicAction<AbstractVersionedTree>, Set<T>> presentMap = new HashMap<>();
    // protected Map<Object, Set<T>> absentMap = new HashMap<>();
    protected Map<MyAction<AbstractVersionedTree>, Boolean> reqState = new HashMap<>(); // false by default
    protected Set<T> validable = new LinkedHashSet<>();
    // protected Consumer<Set<T>> validityLauncher;
	public final Clusterizer globalClusterizer;

    // public void setValidityLauncher(Consumer<Set<T>> validityLauncher) {
    //     this.validityLauncher = validityLauncher;
    // }

    protected EvoStateMaintainer(Version initialState, Map<T, Set<T>> evoToEvo) {
        this.initialState = initialState;
        this.evoToEvo = evoToEvo;
        for (T e : evoToEvo.keySet()) {
            markRequirements(new Chain<>(e));
        }
        for (Set<T> values : presentMap.values()) {
            for (T evolution : values) {
                evoReqSize.put(evolution, evoReqSize.getOrDefault(evolution, 0) + 1);
            }
        }
        Set<AtomicAction<AbstractVersionedTree>> allAA = new LinkedHashSet<>();
        for (T evo : evoToEvo.keySet()) {
            Object ori = getOriginal(evo);
            if (ori instanceof MyAction) {
                decompose(allAA, (MyAction<?>)ori);
            }
        }
        this.globalClusterizer = new Clusterizer(allAA);
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
        getValidityLauncher().accept(Collections.unmodifiableSet(new LinkedHashSet<>(validable)));
    }

    public Fraction ratio(T e) {
        return Fraction.getFraction(evoState.get(e), evoReqSize.get(e));
    }

    public Set<T> getEvolutions(Set<T> e) {
        Set<T> r = new LinkedHashSet<>();
        getEvolutions(e, r);
        return r;
    }

    protected void getEvolutions(Set<T> evos, Set<T> r) {
        for (T e : evos) {
            r.add(e);
            if (evoToEvo.containsKey(e)) {
                getEvolutions(evoToEvo.get(e), r);
            }
        }
    }

    public Set<MyAction<AbstractVersionedTree>> getActions(Set<T> e) {
        Set<MyAction<AbstractVersionedTree>> r = new LinkedHashSet<>();
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

    protected void markRequirements(Chain<T> evos) {
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

    protected void coarseDecompose(Set<MyAction<?>> r, MyAction<?> a) {
        if (a instanceof AtomicAction) {
            r.add(a);
        } else {
            for (MyAction<?> ca : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                r.add(ca);
                coarseDecompose(r, ca);
            }
        }
    }
    protected void decompose(Set<AtomicAction<AbstractVersionedTree>> r, MyAction<?> a) {
        if (a instanceof AtomicAction) {
            r.add((AtomicAction)a);
        } else {
            for (MyAction<?> ca : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                decompose(r, ca);
            }
        }
    }

    protected void markRequirements(AtomicAction<AbstractVersionedTree> a, Collection<T> e) {
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

    protected void markInsertRequirements(MyAction<AbstractVersionedTree> a, Collection<T> e) {
        // AbstractVersionedTree t = a.getTarget();
        // assert (t != null);
        // // AbstractVersionedTree p = t.getParent();
        // presentMap.putIfAbsent(t, new HashSet<>());
        // presentMap.get(t).addAll(e);
    }

    protected void markDeleteRequirements(MyAction<AbstractVersionedTree> a, Collection<T> e) {
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