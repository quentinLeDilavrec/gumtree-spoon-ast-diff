
/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.actions;

import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.actions.MyAction.CompressibleAtomicAction;
import com.github.gumtreediff.actions.MyAction.MyMove;
import com.github.gumtreediff.actions.MyAction.Scriptable;
import com.github.gumtreediff.actions.MyAction.Scripted;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;

import org.apache.commons.compress.utils.Iterators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Class representing edit scripts: sequence of edit actions.
 *
 * @see Action
 */
public class EditScript<T extends ITree> implements Iterable<MyAction<?>> {

    private class Intern<AA extends Action & AtomicAction<T>> {
        private Map<T, AA> tree2AtomicAction = new HashMap<>();
        private Map<T, List<AA>> atomicsUniformWaitingForParent = new HashMap<>();
        private Map<MyAction<?>, ComposedAction<T>> component2ComposedAction = new HashMap<>();
        private Map<AA, Integer> actionChildNotSeenUniform = new HashMap<>();
        private Map<ComposedAction<T>, Integer> actionComponentNotSeenUniform = new HashMap<>();
        private Set<AA> possiblyAtomicUniform = new HashSet<>();
        private Set<AA> summarizedAtomics = new HashSet<>();
        private Set<MyAction<?>> summarizedButComposed = new HashSet<>();
        private Set<ComposedAction<T>> summarizedComposed = new HashSet<>();
        private Set<MyAction<?>> waitingComposition = new HashSet<>();

        private void addAux(MyAction<?> action) {
            if (action instanceof AtomicAction) {
                T target = ((AtomicAction<T>) action).getTarget();
                AtomicAction<T> old = tree2AtomicAction.put(target, (AA) action);
                if (old == null) {
                    actionChildNotSeenUniform.merge((AA) action, getChildren(target).size(), Integer::sum);
                    possiblyAtomicUniform.add((AA) action);
                } else {
                    assert old.getClass().equals(action.getClass());
                    assert old == action;
                }
            } else if (action instanceof ComposedAction) {
                actionComponentNotSeenUniform.putIfAbsent((ComposedAction<T>) action, 0);
                for (MyAction<?> component : ((ComposedAction<T>) action).composed()) {
                    boolean nw = parOfThisScript(component);
                    MyAction<?> old = component2ComposedAction.put(component, (ComposedAction<T>) action);
                    assert old == null;
                    actionComponentNotSeenUniform.put((ComposedAction<T>) action,
                            actionComponentNotSeenUniform.get(action) + 1);
                    // addAux(component);
                }
            }
        }

        Set<T> alreadyCompressed = new HashSet<>();

        /**
         * It compresses Atomic actions ie. (uniform && top) || (-uniform)
         * Should be called in reverse breath first order on tree
         * @param target in analized tree
         */
        public void compressAtomic(T target) {
            if (alreadyCompressed.contains(target)) {
                throw null;
            }
            alreadyCompressed.add(target);
            AA action = tree2AtomicAction.get(target);
            T parent = (T) target.getParent();
            AA parentAction = parent == null ? null : tree2AtomicAction.get(parent);
            if (action == null && parentAction == null) {
            } else if (parentAction == null) {
                if (action != null && possiblyAtomicUniform.contains(action)) {
                    if (action instanceof CompressibleAtomicAction) {
                        ((CompressibleAtomicAction<T>) action).setCompressed(
                                (List) atomicsUniformWaitingForParent.getOrDefault(target, new ArrayList<>()));
                    }
                    summarizedAtomics.add(action);
                }
                extracted(action);
            } else if (action == null) {
                // parentAction cannot be uniform
                boolean removed = possiblyAtomicUniform.remove(parentAction);
                if (removed) {
                    summarizedAtomics.add(parentAction);
                    for (AA a : atomicsUniformWaitingForParent.getOrDefault(parent,
                            (List<AA>) Collections.EMPTY_LIST)) {
                        summarizedAtomics.add(a);
                    }
                    atomicsUniformWaitingForParent.remove(parent);
                }
            } else if (action.getClass().equals(parentAction.getClass())) { // same type actions
                if (possiblyAtomicUniform.contains(parentAction)) {
                    if (possiblyAtomicUniform.contains(action)) {
                        atomicsUniformWaitingForParent.putIfAbsent(parent, new ArrayList<>());
                        atomicsUniformWaitingForParent.get(parent).add(action);
                        if (action instanceof CompressibleAtomicAction) {
                            ((CompressibleAtomicAction<T>) action).setCompressed(
                                    (List) atomicsUniformWaitingForParent.getOrDefault(target, new ArrayList<>()));
                        }
                        ComposedAction<T> composedAction = component2ComposedAction.get(action);
                        if (composedAction != null) {
                            waitingComposition.add(action);
                        }
                    }
                } else if (possiblyAtomicUniform.contains(action)) {
                    summarizedAtomics.add(action);
                    if (action instanceof CompressibleAtomicAction) {
                        ((CompressibleAtomicAction<T>) action).setCompressed(
                                (List) atomicsUniformWaitingForParent.getOrDefault(target, new ArrayList<>()));
                    }
                    // possiblyAtomicUniform.remove(action);
                }
            } else { // not same type
                // parentAction cannot be uniform
                boolean removed = possiblyAtomicUniform.remove(parentAction);
                if (removed) {
                    summarizedAtomics.add(parentAction);
                    extracted(action);
                    for (AA a : atomicsUniformWaitingForParent.getOrDefault(parent,
                            (List<AA>) Collections.EMPTY_LIST)) {
                        summarizedAtomics.add(a);
                        extracted(a);
                    }
                    atomicsUniformWaitingForParent.remove(parent);
                }
                if (possiblyAtomicUniform.contains(action)) {
                    // thus this action must be a sumarized action
                    summarizedAtomics.add(action);
                    // atomicsWaitingForParent.remove(target); // maybe used in some unsummarization for composed
                    // possiblyAtomicUniform.remove(action);
                    extracted(action);
                }
            }
        }

        private void extracted(AA action) {
            ComposedAction<T> composedAction = component2ComposedAction.get(action);
            if (composedAction != null) {
                Integer val = actionComponentNotSeenUniform.merge(composedAction, -1, Integer::sum);
                if (val == 0) {
                    promoteComposedAction(composedAction);
                }
            }
        }

        private void promoteComposedAction(ComposedAction<T> composedAction) {
            summarizedComposed.add(composedAction);
            actionComponentNotSeenUniform.remove(composedAction);
            for (MyAction a : composedAction.composed()) {
                if (a instanceof ComposedAction) {
                    summarizedAtomics.remove(a);
                } else if (a instanceof AtomicAction) {
                    summarizedAtomics.remove(a);
                }
                summarizedButComposed.add(a);
            }
        }

        private void compressAux(ComposedAction<T> action) {
            if (action instanceof MyMove) {
                List<AtomicAction<AbstractVersionedTree>> composeds = ((MyMove) action).composed();
                for (AtomicAction<AbstractVersionedTree> component : composeds) {
                    if (component instanceof AtomicAction) {
                        for (AtomicAction<AbstractVersionedTree> compressed : ((AtomicAction<AbstractVersionedTree>) component)
                                .getCompressed()) {
                            ComposedAction<T> ca = this.component2ComposedAction.get(compressed);
                            if (ca == null) {
                                summarizedAtomics.add((AA) compressed);
                            } else {
                                compressAux(ca);
                            }
                        }
                    }
                }
            }
        }

        public void compressComposed() {
            for (MyAction<?> action : addedActions) {
                if (summarizedComposed.contains(action)) {
                    compressAux((ComposedAction<T>) action);
                } else if (summarizedAtomics.contains(action)) {
                    ComposedAction<T> composedAction = component2ComposedAction.get(action);
                    if (composedAction != null) {
                        int c = 0;
                        for (MyAction<?> a : composedAction.composed()) {
                            c--;
                            if (summarizedAtomics.contains(a)) {
                                c++;
                            } else if (waitingComposition.contains(a)) {
                                c++;
                            }
                        }
                        if (c != 0) {
                            continue;
                        }
                        int others = 0;
                        int thisone = 0;
                        int othersC = 0;
                        int thisoneC = 1;
                        for (MyAction<?> a : composedAction.composed()) {
                            // TODO composed action made of some composed actions
                            if (a instanceof AtomicAction) {
                                thisone += 1;
                                for (AtomicAction<T> myAction : ((AtomicAction<T>) a).getCompressed()) {
                                    thisone += countValue(myAction);
                                }
                                ITree parent = ((AtomicAction<T>) a).getTarget().getParent();
                                AtomicAction<T> ap = tree2AtomicAction.get(parent);
                                while (ap != null) {
                                    if (summarizedAtomics.contains(ap)) {
                                        others += 1;
                                        othersC += 1;
                                        for (AtomicAction<T> myAction : ap.getCompressed()) {
                                            others += countValue(myAction);
                                        }
                                        break;
                                    } else {
                                        parent = parent.getParent();
                                        ap = tree2AtomicAction.get(parent);
                                    }
                                }
                            }
                        }
                        if ((thisoneC >= othersC && composedAction instanceof MyMove) || others < thisone) {
                            promoteComposedAction(composedAction);
                        }
                    }
                }
            }
        }

        private int countValue(AtomicAction<T> a) {
            int r = 0;
            if (!summarizedAtomics.contains((AA) a)) {
                r += 1;
                for (AtomicAction<T> aa : a.getCompressed()) {
                    r += countValue(aa);
                }
            }
            return r;
        }
    }

    private final EditScript<T>.Intern<?> INTERN = new Intern<>();

    private List<MyAction<?>> addedActions = new ArrayList<>();
    // private Set<Action> notTopLevelComposed = new HashSet<>();
    // private Set<Action> topLevelUniformComposed = new HashSet<>();
    private boolean compressed = false;

    EditScript() {
    }

    /**
     * @return summarized actions, nonetheless complete
     */
    public Iterator<MyAction<?>> iterator() {
        assert compressed;
        // return Iterators2.createChainIterable(Arrays.asList(topLevelUniformComposed, topLevelNotComposed)).iterator();
        return addedActions.iterator();
    }

    public int size() {
        assert compressed;
        // return topLevelUniformComposed.size() + topLevelNotComposed.size();
        return addedActions.size();
    }

    void add(MyAction<?> action) {
        boolean nw = parOfThisScript(action);
        addedActions.add(action);
        INTERN.addAux(action);
    }

    /**
     * @param target
     * @return children left and right in intermediate tree
     */
    protected List<T> getChildren(T target) {
        return (List<T>) target.getChildren();
    }

    // void addComponent(Action action) {

    // }

    private boolean parOfThisScript(MyAction<?> action) {
        if (action instanceof Scriptable) {
            if (((Scriptable<EditScript<T>>) action).getEditScript() == null) {
                ((Scriptable<EditScript<T>>) action).setEditScript(this);
                return true;
            } else {
                return ((Scriptable<EditScript<T>>) action).getEditScript() == this;
            }
        } else {
            return false;
        }
    }

    // public Action get(int index) {
    //     return actions().get(index);
    // }

    // public boolean remove(Action action) {
    //     return actions().remove(action);
    // }

    // public Action remove(int index) {
    //     return actions().remove(index);
    // }

    public List<MyAction<?>> asList() {
        List<MyAction<?>> r = new ArrayList<>();
        addedActions.stream().forEach(new Consumer<MyAction<?>>() {
            @Override
            public void accept(MyAction<?> x) {
                if (x instanceof AtomicAction) {
                    if (INTERN.summarizedAtomics.contains(x)) {
                        r.add(x);
                    }
                } else if (x instanceof ComposedAction) {
                    if (INTERN.summarizedComposed.contains(x)) {
                        r.add(x);
                    } else {
                        for (MyAction<?> y : ((ComposedAction<T>) x).composed()) {
                            accept(y);
                        }
                    }
                } else {
                    throw null;
                }
            }
        });
        return r;
    }

    public <U extends Action & ComposedAction<T>> Set<U> getComposed() {
        return INTERN.summarizedComposed.stream().map(x -> (U) x).collect(Collectors.toUnmodifiableSet());
    }

    public <U extends Action & AtomicAction<T>> List<U> getAtomic() {
        List<U> r = new ArrayList<>();
        addedActions.stream().forEach(new Consumer<MyAction<?>>() {
            @Override
            public void accept(MyAction<?> x) {
                if (x instanceof AtomicAction) {
                    if (INTERN.summarizedAtomics.contains(x)) {
                        r.add((U) x);
                    }
                    if (INTERN.summarizedButComposed.contains(x)) {
                        r.add((U) x);
                    }
                }
            }
        });
        return r;
    }

    /**
     * for now only handle actions, on one hand,
     * atomic, ie. at most one per node and only afecting one node
     * composed, ie. an action can be a component of at most one composed action
     */
    public void compress() {
        if (!compressed) {
            INTERN.compressComposed();

            compressed = true;
        }
    }

    /**
     * It compresses Atomic actions ie. (uniform && top) || (-uniform)
     * Should be called in reverse breath first order on tree
     * @param target in analized tree
     */
    public void compressAtomic(T target) {
        INTERN.compressAtomic(target);
    }
}

class Iterators2 {

    public static <I> Iterable<I> createChainIterable(Iterable<Iterable<I>> input) {
        return new Iterable<I>() {
            @Override
            public Iterator<I> iterator() {
                return new Iterators2.ChainIterable<>(input);
            }
        };
    }

    private static class ChainIterable<I> implements Iterator<I> {
        private final Iterator<Iterable<I>> inputs;
        private Iterator<I> current;

        ChainIterable(Iterable<Iterable<I>> inputs) {
            this.inputs = inputs.iterator();
        }

        @Override
        public boolean hasNext() {
            if (current != null && current.hasNext()) {
                return true;
            } else {
                while (inputs.hasNext()) {
                    current = inputs.next().iterator();
                    if (current.hasNext()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public I next() {
            if (hasNext()) {
                return current.next();
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}