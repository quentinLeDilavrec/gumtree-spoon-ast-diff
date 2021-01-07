package gumtree.spoon.apply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;

import org.apache.commons.lang3.tuple.ImmutablePair;

import gumtree.spoon.apply.operations.MyScriptGenerator;

public interface Flattener {

    public List<ImmutablePair<Integer, Clusterizer.Cluster>> getConstrainedTree();

    public class Clusterizer implements Flattener {
        public final class Cluster {
            private AbstractVersionedTree root;
            private AbstractVersionedTree maybePresentParent;
            private LinkedHashSet<AbstractVersionedTree> nodes = new LinkedHashSet<>();
    
            private Cluster(AbstractVersionedTree root, AbstractVersionedTree maybePresentParent) {
                this.root = root;
                this.maybePresentParent = maybePresentParent;
                this.nodes = new LinkedHashSet<>();
                this.nodes.add(root);
                childCache.putIfAbsent(maybePresentParent, new LinkedHashSet<>());
                childCache.get(maybePresentParent).add(this);
            }
    
            public AbstractVersionedTree getMaybePresentParent() {
                return maybePresentParent;
            }
    
            public Set<AbstractVersionedTree> getNodes() {
                return Collections.unmodifiableSet(nodes);
            }
    
            public AbstractVersionedTree getRoot() {
                return root;
            }
    
            // public Set<Cluster> getChildren() {
            //     LinkedHashSet<Cluster> r = new LinkedHashSet<>();
            //     for (AbstractVersionedTree x : nodes) {
    
            //     }
            //     return r;
            // }
        }
    
        private final Cluster makeClust(AbstractVersionedTree root, AbstractVersionedTree maybePresentParent) {
            return new Cluster(root,maybePresentParent);
        }
    
        protected final Cluster makeClust(AbstractVersionedTree root, AbstractVersionedTree maybePresentParent, LinkedHashSet<AbstractVersionedTree> secondaries) {
            Cluster r = makeClust(root,maybePresentParent);
            r.nodes.addAll(secondaries);
            return r;
        }
    
        protected final Map<AbstractVersionedTree, Set<Cluster>> maybePresentNodes;
        protected final Map<AbstractVersionedTree, Set<Cluster>> childCache;
        protected final Set<MyAction<?>> actions;

        public Set<MyAction<?>> getActions() {
            return Collections.unmodifiableSet(actions);
        }
    
        public Set<Cluster> getClusters() {
            throw null;
        }
    
        public Clusterizer(Set<AtomicAction<AbstractVersionedTree>> allPossiblyConsidered) {
            this.actions = new LinkedHashSet<>((Set) allPossiblyConsidered);
            this.maybePresentNodes = new HashMap<>();
            this.childCache = new HashMap<>();
            for (AtomicAction<AbstractVersionedTree> aa : allPossiblyConsidered) {
                if (maybePresentNodes.containsKey(aa.getTarget())) {
                    throw null;
                }
                maybePresentNodes.put(aa.getTarget(), new LinkedHashSet<>());
            }
            for (AbstractVersionedTree k : maybePresentNodes.keySet()) {
                makeClustR(k, k.getParent());
            }
        }
        // private void makeCluster(Cluster c, AbstractVersionedTree n) {
        //     Set<Cluster> vals = maybePresentNodes.get(n);
        //     if (vals != null) {
        //         assert vals.size() == 1;
        //         for (Cluster val : vals) {
        //             c.maybePresentParent = n;
        //             childClusters.putIfAbsent(val, new HashSet<>());
        //             childClusters.get(val).add(c);
        //             break;
        //         }
        //     } else if (n.getParent() != null) {
        //         makeCluster(c, n.getParent());
        //     }
        // }
    
        private void makeClustR(AbstractVersionedTree k, AbstractVersionedTree p) {
            if (maybePresentNodes.containsKey(p)) {
                maybePresentNodes.get(k).add(new Cluster(k, p));
            } else if (p.getParent() != null) {
                makeClustR(k, p.getParent());
            } else {
                maybePresentNodes.get(k).add(new Cluster(k, null));
            }
        }
    
        public Clusterizer(Clusterizer original, Set<AtomicAction<AbstractVersionedTree>> wanted) {
            assert original.actions.containsAll(wanted);
            this.actions = new LinkedHashSet<>();
            this.maybePresentNodes = new HashMap<>();
            this.childCache = new HashMap<>();
            Set<AbstractVersionedTree> targets = new LinkedHashSet<>();
            for (AtomicAction<AbstractVersionedTree> aa : wanted) {
                targets.add(aa.getTarget());
                // populate parents wanted in original
                for (ITree t : aa.getTarget().getParents()) {
                    if (original.maybePresentNodes.containsKey(t) && original.actions.contains(t.getMetadata(MyScriptGenerator.INSERT_ACTION))) {
                        targets.add((AbstractVersionedTree)t);
                    }
                }
                if (aa instanceof MyAction.MyDelete) {
                    aux(original, aa.getTarget(), targets);
                }
            }
            for (Entry<AbstractVersionedTree, Set<Cluster>> entry : original.maybePresentNodes.entrySet()) {
                for (Cluster c : entry.getValue()) {
                    if (targets.containsAll(c.getNodes())) {
                        this.maybePresentNodes.putIfAbsent(entry.getKey(), new LinkedHashSet<>());
                        this.maybePresentNodes.get(entry.getKey()).add(c);
                        this.childCache.putIfAbsent(c.maybePresentParent, new LinkedHashSet<>());
                        this.childCache.get(c.maybePresentParent).add(c);
                    }
                }
            }
        }
    
        private void aux(Clusterizer original, AbstractVersionedTree target, Set<AbstractVersionedTree> targets) {
            for (AbstractVersionedTree t : target.getAllChildren()) {
                if(this.maybePresentNodes.containsKey(t))
                    continue;
                if (original.maybePresentNodes.containsKey(t)) {
                    if (original.actions.contains(t.getMetadata(MyScriptGenerator.DELETE_ACTION))) {
                        targets.add((AbstractVersionedTree)t);
                    }
                } else {
                    aux(original, t, targets);
                }
            }
        }

        // public boolean isWanted(MyAction action) {
        //     return this.wanted.contains(action);
        // }
        @Override
        public List<ImmutablePair<Integer, Cluster>> getConstrainedTree() {
            List<Cluster> r = new ArrayList<>();
            Map<AbstractVersionedTree, Integer> avt2Index = new HashMap<>();
            avt2Index.put(null, -1);
            Queue<Cluster> remaining = new LinkedList<>();
            remaining.add(null);
            while (!remaining.isEmpty()) {
                Cluster curr = remaining.poll();
                List<Cluster> childs = new ArrayList<>();
                for (AbstractVersionedTree n : curr.getNodes()) {
                    childs.addAll(childCache.getOrDefault(n, new HashSet<>()));
                }
                childs.sort((a, b) -> b.getNodes().size() - a.getNodes().size());
                if (curr == null) {
                    for (Cluster c : childs) {
                        remaining.add(c);
                    }
                } else if (avt2Index.containsKey(curr.getMaybePresentParent())) {
                    boolean b = true;
                    for (AbstractVersionedTree n : curr.getNodes()) {
                        if (avt2Index.containsKey(n)) {
                            b = false;
                            break;
                        }
                        avt2Index.put(n, r.size());
                    }
                    if (b) {
                        r.add(curr);
                        for (Cluster c : childs) {
                            remaining.add(c);
                        }
                    } else {
                        System.err.println();
                    }
                } else {
                    remaining.add(curr);
                }
            }
            if (avt2Index.size() != maybePresentNodes.size() + 1) {
                System.err.println("avt2Index.size() != actions.size() + 1:");
                for (AbstractVersionedTree x : avt2Index.keySet()) {
                    System.err.println(x);                    
                }
                for (AbstractVersionedTree x : maybePresentNodes.keySet()) {
                    System.err.println(x);
                }
            }
            List<ImmutablePair<Integer, Cluster>> rr = r.stream()
                    .map(x -> new ImmutablePair<>(avt2Index.get(x.getMaybePresentParent()), x)).collect(Collectors.toList());
            return rr;
        }
    }
    
    class ComposingClusterizer extends Clusterizer {
        private final Map<Cluster, Set<Cluster>> composingCache;
        private Set<Cluster> inibiteds;
        public ComposingClusterizer(Clusterizer original, Set<AtomicAction<AbstractVersionedTree>> wanted) {
            super(original, wanted);
            if (original instanceof ComposingClusterizer) {
                this.composingCache = new HashMap<>();
                for (Entry<Cluster, Set<Cluster>> entry : ((ComposingClusterizer) original).composingCache.entrySet()) {
                    if (this.maybePresentNodes.getOrDefault(entry.getKey().getRoot(), Collections.EMPTY_SET).contains(entry.getKey())) {
                        this.composingCache.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
                    }
                }
            } else {
                this.composingCache = new HashMap<>();
            }
        }
    
        protected Cluster compose(Cluster primary, Cluster... secondaries) {
            LinkedHashSet<AbstractVersionedTree> secondaryNodes = new LinkedHashSet<>();
            secondaryNodes.addAll(primary.getNodes());
            for (Cluster secondary : secondaries) {
                secondaryNodes.addAll(secondary.getNodes());
            }
            Cluster r = makeClust(primary.getRoot(), primary.getMaybePresentParent(), secondaryNodes);
            composingCache.putIfAbsent(r, new LinkedHashSet<>());
            composingCache.get(r).add(primary);
            for (Cluster secondary : secondaries) {
                composingCache.get(r).add(secondary);
            }
            return r;
        }
    
        private Cluster compose(Cluster c1, Cluster c2) {
            AbstractVersionedTree primary;
            AbstractVersionedTree mpp;
            LinkedHashSet<AbstractVersionedTree> nodes = new LinkedHashSet<>();
            if (c1.getNodes().contains(c2.getMaybePresentParent())) { // could be ? c1.root.equals(c2.getMaybePresentParent())
                // c2 can be composed into c1
                primary = c1.getRoot();
                mpp = c1.getMaybePresentParent();
                nodes.addAll(c1.getNodes());
                nodes.addAll(c2.getNodes());
            } else if (c2.getNodes().contains(c1.getMaybePresentParent())) { // idem with c2.getRoot().equals(c1.getMaybePresentParent()) ?
                // c1 can be composed into c2
                primary = c2.getRoot();
                mpp = c2.getMaybePresentParent();
                nodes.addAll(c2.getNodes());
                nodes.addAll(c1.getNodes());
            } else {
                AbstractVersionedTree pc1 = c1.getMaybePresentParent();
                AbstractVersionedTree pc2 = c2.getMaybePresentParent();
                if (pc1 == pc2) {
                    // same constraining parent
                    primary = c1.getRoot();
                    mpp = c1.getMaybePresentParent();
                    nodes.addAll(c1.getNodes());
                    nodes.addAll(c2.getNodes());
                } else if (pc2 == null || (pc1 != null && isInLineage(pc2, pc1, new HashSet<>()))) {
                    // c1 could be more constrained
                    primary = c1.getRoot();// for now
                    mpp = c1.getMaybePresentParent();
                    nodes.addAll(c1.getNodes());
                    nodes.addAll(c2.getNodes());
                } else if (pc1 == null || isInLineage(pc1, pc2, new HashSet<>())) {
                    // c2 could be more constrained
                    primary = c2.getRoot();// for now
                    mpp = c2.getMaybePresentParent();
                    nodes.addAll(c2.getNodes());
                    nodes.addAll(c1.getNodes());
                } else {
                    // cannot be merged (at least for now)
                    // could be merged if some respective parents were merged
                    // maybe store ppc1 and ppc2 along with c1 and c2 to do the merge if the possibility presents itself
                    return null;
                }
            }
            return makeClust(primary, mpp, nodes);
        }
    
        private Cluster composeReversedLeaf(Cluster c1, Cluster toReverse) {
            // TODO
            throw null;
        }
    
        private Cluster compose(Set<Cluster> cs) {
            AbstractVersionedTree primary = null;
            AbstractVersionedTree mpp = null;
            LinkedHashSet<AbstractVersionedTree> nodes = new LinkedHashSet<>();
            Set<AbstractVersionedTree> needed = new HashSet<>();
            for (Cluster c : cs) {
                if (primary == null || mpp == c.getRoot()) {
                    primary = c.getRoot();
                    mpp = c.getMaybePresentParent();
                }
                needed.add(mpp);
                nodes.addAll(c.getNodes());
            }
            needed.removeAll(nodes);
            if (needed.size() > 1) {
                throw null;
            } else if (needed.size() == 1 && !needed.contains(null)) {
                throw null;
            }
            return makeClust(primary, mpp, nodes);
        }
    
        // x must not be null
        private boolean isInLineage(AbstractVersionedTree x, AbstractVersionedTree n, Set<AbstractVersionedTree> cache) {
            if (n == null || cache.contains(n)) {
                return false;
            }
            for (Cluster c : maybePresentNodes.get(n)) {
                if (cache.contains(c.getMaybePresentParent())) {
                    continue;
                } else if (c.getMaybePresentParent() == x) {
                    return true;
                } else if (isInLineage(x, c.getMaybePresentParent(), cache)) {
                    return true;
                }
            }
            cache.add(n);
            return false;
        }
    
        public void clusterize(ComposedAction<AbstractVersionedTree> ca) {
            clusterizeAux(ca);
        }
    
        private Set<AtomicAction<AbstractVersionedTree>> clusterizeAux(ComposedAction<AbstractVersionedTree> ca) {
            Set<AtomicAction<AbstractVersionedTree>> aas = new HashSet<>();
            Set<AbstractVersionedTree> targets = ca.getTarget();
            for (MyAction<?> a : ca.composed()) {
                if (a instanceof ComposedAction) {
                    aas.addAll(clusterizeAux((ComposedAction) a));
                } else if (a instanceof AtomicAction) {
                    aas.add((AtomicAction) a);
                } else {
                    throw null;
                }
            }
            Set<Cluster> mergeCandidates = new HashSet<>();
            for (AtomicAction<AbstractVersionedTree> aa : aas) {
                assert actions.contains(aa);
                Set<Cluster> tmp = this.maybePresentNodes.get(aa.getTarget());
                tmp.removeIf(x -> !targets.containsAll(x.getNodes()));
                mergeCandidates.addAll(tmp);
            }
            Cluster composed;
            if (mergeCandidates.size() > 1 && !mergeCandidates.stream().anyMatch(x -> x.getNodes().containsAll(targets))) {
                if (mergeCandidates.size() == 2) {
                    Iterator<Cluster> it = mergeCandidates.iterator();
                    composed = compose(it.next(), it.next());
                } else {
                    composed = compose(mergeCandidates);
                }
                if (composed != null) {
                    // this.clusters.add(composed);
                    this.composingCache.put(composed, new LinkedHashSet<>(mergeCandidates));
                    this.childCache.putIfAbsent(composed.getMaybePresentParent(), new LinkedHashSet<>());
                    this.childCache.get(composed.getMaybePresentParent()).add(composed);
                    for (AbstractVersionedTree n : composed.getNodes()) {
                        this.maybePresentNodes.get(n).add(composed);
                    }
                }
            }
            return aas;
        }
    
        public Set<Cluster> getCluster(ComposedAction<AbstractVersionedTree> ca) {
            Set<Cluster> r = new HashSet<>();
            Set<AbstractVersionedTree> targets = ca.getTarget();
            Set<Cluster> candidates = null;
            for (AbstractVersionedTree n : targets) {
                if (candidates == null) {
                    candidates = new HashSet<>(maybePresentNodes.get(n));
                } else {
                    candidates.retainAll(maybePresentNodes.get(n));
                }
            }
            if (candidates != null) {
                r.addAll(candidates);
            }
            return r;
        }
    
        public void setInibiteds(Set<Cluster> inibiteds) {
            this.inibiteds = inibiteds;
        }
    
        @Override
        public List<ImmutablePair<Integer, Cluster>> getConstrainedTree() {
            List<Cluster> r = new ArrayList<>();
            Map<AbstractVersionedTree, Integer> avt2Index = new HashMap<>();
            avt2Index.put(null, -1);
            Queue<Cluster> remaining = new LinkedList<>();
            remaining.add(null);
            while (!remaining.isEmpty()) {
                Cluster curr = remaining.poll();
                if (curr == null) {
                    List<Cluster> childs = new ArrayList<>(childCache.getOrDefault(curr, Collections.emptySet()));
                    childs.sort((a, b) -> b.getNodes().size() - a.getNodes().size());
                    for (Cluster c : childs) {
                        remaining.add(c);
                    }
                } else if (inibiteds.contains(curr)) {
                    List<Cluster> compo = new ArrayList<>(composingCache.getOrDefault(curr, Collections.emptySet()));
                    compo.sort((a, b) -> b.getNodes().size() - a.getNodes().size());
                    remaining.addAll(compo);
                } else if (avt2Index.containsKey(curr.getMaybePresentParent())) {
                    boolean ok = true;
                    for (AbstractVersionedTree n : curr.getNodes()) {
                        if (avt2Index.containsKey(n)) {
                            ok = false;
                            break;
                        }
                        avt2Index.put(n, r.size());
                    }
                    if (ok) {
                        r.add(curr);
                        List<Cluster> childs = new ArrayList<>();
                        for (AbstractVersionedTree n : curr.getNodes()) {
                            childs.addAll(childCache.getOrDefault(n, Collections.emptySet()));
                        }
                        childs.removeIf(x->{
                            Set<AbstractVersionedTree> tmp = new HashSet<>(x.getNodes());
                            tmp.retainAll(curr.getNodes());
                            return tmp.size()!=0;
                        });
                        childs.sort((a, b) -> b.getNodes().size() - a.getNodes().size());
                        for (Cluster c : childs) {
                            remaining.add(c);
                        }
                    } else {
                        System.out.println("skipped " + curr);
                    }
                } else {
                    remaining.add(curr);
                }
            }
            if (avt2Index.size() != maybePresentNodes.size() + 1) {
                System.err.println("avt2Index.size() != actions.size() + 1:");
                for (AbstractVersionedTree x : avt2Index.keySet()) {
                    System.err.println(x);                    
                }
                for (AbstractVersionedTree x : maybePresentNodes.keySet()) {
                    System.err.println(x);
                }
            }
            List<ImmutablePair<Integer, Cluster>> rr = r.stream()
                    .map(x -> new ImmutablePair<>(avt2Index.get(x.getMaybePresentParent()), x)).collect(Collectors.toList());
            return rr;
        }
    }
}



// public class Flattener {

//     private Map<AtomicAction<AbstractVersionedTree>, Set<ComposedAction<AbstractVersionedTree>>> composedActions = new HashMap<>();
//     private Map<AbstractVersionedTree, AtomicAction<AbstractVersionedTree>> targetToAAction = new HashMap<>();
//     private Map<Cluster, Set<Cluster>> composedClusters = new HashMap<>();

//     public Flattener(Set<AtomicAction<AbstractVersionedTree>> wanted) {
//         this.wanted = new HashSet<>();
//         for (AtomicAction<AbstractVersionedTree> aa : wanted) {
//             this.wanted.add(aa);
//             assert !targetToAAction.containsKey(aa.getTarget());
//             targetToAAction.put(aa.getTarget(), aa);
//         }
//     }

//     private Set<AtomicAction<AbstractVersionedTree>> wanted;

//     public void compute() {
//         clusters = new HashSet<>();
//         maybePresentNodes = new HashMap<>();
//         initiallyPresentNodes = new HashSet<>();
//         prepareClusters();
//         childClusters = new HashMap<>();
//         prepareChildH();
//     }

//     private void prepareClusters() {
//         for (AtomicAction<AbstractVersionedTree> aa : wanted) {
//             Cluster c = new Cluster();
//             c.root = aa.getTarget();
//             c.nodes.add(aa.getTarget());
//             if (aa.getTarget().getMetadata(MyScriptGenerator.DELETE_ACTION) == aa) {
//                 initiallyPresentNodes.add(aa.getTarget());
//                 // c.initiallyPresentNodes.add(aa.getTarget());
//             }
//             clusters.add(c);
//             assert !maybePresentNodes.containsKey(aa.getTarget()) : aa;
//             HashSet<Cluster> value = new HashSet<>();
//             value.add(c);
//             maybePresentNodes.put(aa.getTarget(), value);
//         }
//     }

//     private Set<AbstractVersionedTree> initiallyPresentNodes;
//     private Set<Cluster> clusters;
//     private Map<AbstractVersionedTree, Set<Cluster>> maybePresentNodes;

//     private void prepareMaybePresentParent(Cluster c, AbstractVersionedTree n) {
//         Set<Cluster> vals = maybePresentNodes.get(n);
//         if (vals != null) {
//             assert vals.size() == 1;
//             for (Cluster val : vals) {
//                 c.maybePresentParent = n;
//                 childClusters.putIfAbsent(val, new HashSet<>());
//                 childClusters.get(val).add(c);
//                 break;
//             }
//         } else if (n.getParent() != null) {
//             prepareMaybePresentParent(c, n.getParent());
//         }
//     }

//     public void prepareChildH() {
//         for (Entry<AbstractVersionedTree, Set<Cluster>> entry : maybePresentNodes.entrySet()) {
//             assert entry.getValue().size() == 1;
//             Cluster c = entry.getValue().iterator().next();
//             AbstractVersionedTree n = entry.getKey();
//             prepareMaybePresentParent(c, n.getParent());
//         }
//     }

//     private Map<Cluster, Set<Cluster>> childClusters;

//     private Cluster compose(Cluster c1, Cluster c2) {
//         Cluster r;
//         if (c1.nodes.contains(c2.maybePresentParent)) { // could be ? c1.root.equals(c2.maybePresentParent)
//             // c2 can be composed into c1
//             r = new Cluster();
//             r.root = c1.root;
//             r.maybePresentParent = c1.maybePresentParent;
//             r.nodes.addAll(c1.nodes);
//             r.nodes.addAll(c2.nodes);
//         } else if (c2.nodes.contains(c1.maybePresentParent)) { // idem with c2.root.equals(c1.maybePresentParent) ?
//             // c1 can be composed into c2
//             r = new Cluster();
//             r.root = c2.root;
//             r.maybePresentParent = c2.maybePresentParent;
//             r.nodes.addAll(c2.nodes);
//             r.nodes.addAll(c1.nodes);
//         } else {
//             AbstractVersionedTree pc1 = c1.maybePresentParent;
//             AbstractVersionedTree pc2 = c2.maybePresentParent;
//             if (pc1 == pc2) {
//                 // same constraining parent
//                 r = new Cluster();
//                 r.root = c1.root;// new AbstractVersionedTree.FakeTree(c1.root, c2.root);
//                 // maybePresentNodes.putIfAbsent(r.root, new HashSet<>());
//                 // maybePresentNodes.get(r.root).add(r);
//                 r.maybePresentParent = c1.maybePresentParent;
//                 r.nodes.addAll(c1.nodes);
//                 r.nodes.addAll(c2.nodes);
//             } else if (pc2 == null || (pc1 != null && isInLineage(pc2, pc1, new HashSet<>()))) {
//                 // c1 could be more constrained
//                 r = new Cluster();
//                 r.root = c1.root;// for now
//                 r.maybePresentParent = c1.maybePresentParent;
//                 r.nodes.addAll(c1.nodes);
//                 r.nodes.addAll(c2.nodes);
//             } else if (pc1 == null || isInLineage(pc1, pc2, new HashSet<>())) {
//                 // c2 could be more constrained
//                 r = new Cluster();
//                 r.root = c2.root;// for now
//                 r.maybePresentParent = c2.maybePresentParent;
//                 r.nodes.addAll(c2.nodes);
//                 r.nodes.addAll(c1.nodes);
//             } else {
//                 // cannot be merged (at least for now)
//                 // could be merged if some respective parents were merged
//                 // maybe store ppc1 and ppc2 along with c1 and c2 to do the merge if the possibility presents itself
//                 return null;
//             }
//         }
//         // r.initiallyPresentNodes.addAll(c1.initiallyPresentNodes);
//         // r.initiallyPresentNodes.addAll(c2.initiallyPresentNodes);
//         return r;

//     }

//     private Cluster composeReversedLeaf(Cluster c1, Cluster toReverse) {
//         // TODO
//         throw null;
//     }

//     private Cluster compose(Set<Cluster> cs) {
//         Cluster r = new Cluster();
//         Set<AbstractVersionedTree> needed = new HashSet<>();
//         for (Cluster c : cs) {
//             if (r.root == null || r.maybePresentParent == c.root) {
//                 r.root = c.root;
//                 r.maybePresentParent = c.maybePresentParent;
//             }
//             needed.add(r.maybePresentParent);
//             r.nodes.addAll(c.nodes);
//         }
//         needed.removeAll(r.nodes);
//         if (needed.size() > 1) {
//             throw null;
//         } else if (needed.size() == 1 && !needed.contains(null)) {
//             throw null;
//         }
//         return r;
//     }

//     // x must not be null
//     private boolean isInLineage(AbstractVersionedTree x, AbstractVersionedTree n, Set<AbstractVersionedTree> cache) {
//         if (n == null || cache.contains(n)) {
//             return false;
//         }
//         for (Cluster c : maybePresentNodes.get(n)) {
//             if (cache.contains(c.maybePresentParent)) {
//                 continue;
//             } else if (c.maybePresentParent == x) {
//                 return true;
//             } else if (isInLineage(x, c.maybePresentParent, cache)) {
//                 return true;
//             }
//         }
//         cache.add(n);
//         return false;
//     }

//     public void clusterize(ComposedAction<AbstractVersionedTree> ca) {
//         // Set<AbstractVersionedTree> ts = ca.getTarget();
//         // for (AbstractVersionedTree n : ts) {
//         //     composedActions.putIfAbsent(n, new HashSet<>());
//         //     composedActions.get(n).add(ca);
//         // }
//         clusterizeAux(ca);
//     }

//     private Set<AtomicAction<AbstractVersionedTree>> clusterizeAux(ComposedAction<AbstractVersionedTree> ca) {
//         Set<AtomicAction<AbstractVersionedTree>> aas = new HashSet<>();
//         Set<AbstractVersionedTree> targets = ca.getTarget();
//         for (MyAction<?> a : ca.composed()) {
//             if (a instanceof ComposedAction) {
//                 aas.addAll(clusterizeAux((ComposedAction) a));
//             } else if (a instanceof AtomicAction) {
//                 composedActions.putIfAbsent((AtomicAction) a, new HashSet<>());
//                 aas.add((AtomicAction) a);
//             } else {
//                 throw null;
//             }
//         }
//         Set<Cluster> mergeCandidates = new HashSet<>();
//         for (AtomicAction<AbstractVersionedTree> aa : aas) {
//             assert wanted.contains(aa);
//             if (composedActions.get(aa).contains(ca)) {
//                 return aas;
//             }
//             composedActions.get(aa).add(ca);
//             Set<Cluster> tmp = this.maybePresentNodes.get(aa.getTarget());
//             tmp.removeIf(x -> !targets.containsAll(x.nodes));
//             mergeCandidates.addAll(tmp);
//         }
//         Cluster composed;
//         if (mergeCandidates.size() > 1 && !mergeCandidates.stream().anyMatch(x -> x.nodes.containsAll(targets))) {
//             if (mergeCandidates.size() == 2) {
//                 Iterator<Cluster> it = mergeCandidates.iterator();
//                 composed = compose(it.next(), it.next());
//             } else {
//                 composed = compose(mergeCandidates);
//             }
//             if (composed != null) {
//                 this.clusters.add(composed);
//                 this.composedClusters.put(composed, mergeCandidates);
//                 for (AbstractVersionedTree n : composed.nodes) {
//                     this.maybePresentNodes.get(n).add(composed);
//                 }
//             }
//         }
//         return aas;
//     }

//     public Set<Cluster> getCluster(ComposedAction<AbstractVersionedTree> ca) {
//         Set<Cluster> r = new HashSet<>();
//         Set<AbstractVersionedTree> targets = ca.getTarget();
//         Set<Cluster> candidates = null;
//         for (AbstractVersionedTree n : targets) {
//             if (candidates == null) {
//                 candidates = new HashSet<>(maybePresentNodes.get(n));
//             } else {
//                 candidates.retainAll(maybePresentNodes.get(n));
//             }
//         }
//         if (candidates != null) {
//             r.addAll(candidates);
//         }
//         return r;
//     }

//     /**
//      * choose clusters by combinatorial complexity
//      * @return
//      */
//     public List<ImmutablePair<Integer, Cluster>> getConstrainedTree(Set<Cluster> toBreak) {
//         linkClusters();
//         List<Cluster> r = new ArrayList<>();
//         Map<AbstractVersionedTree, Integer> avt2Index = new HashMap<>();
//         avt2Index.put(null, -1);
//         Queue<Cluster> remaining = new LinkedList<>();
//         remaining.add(null);
//         while (!remaining.isEmpty()) {
//             Cluster curr = remaining.poll();
//             List<Cluster> childs = new ArrayList<>(childClusters.getOrDefault(curr, new HashSet<>()));
//             childs.sort((a, b) -> b.nodes.size() - a.nodes.size());
//             if (curr == null) {
//                 for (Cluster c : childs) {
//                     remaining.add(c);
//                 }
//             } else if (toBreak.contains(curr)) {
//                 List<Cluster> compo = new ArrayList<>(composedClusters.getOrDefault(curr, new HashSet<>()));
//                 compo.sort((a, b) -> b.nodes.size() - a.nodes.size());
//                 remaining.addAll(compo);
//             } else if (avt2Index.containsKey(curr.maybePresentParent)) {
//                 boolean b = true;
//                 for (AbstractVersionedTree n : curr.nodes) {
//                     if (avt2Index.containsKey(n)) {
//                         b = false;
//                         break;
//                     }
//                     avt2Index.put(n, r.size());
//                 }
//                 if (b) {
//                     r.add(curr);
//                     for (Cluster c : childs) {
//                         remaining.add(c);
//                     }
//                 } else {
//                     System.err.println();
//                 }
//             } else {
//                 remaining.add(curr);
//             }
//         }
//         if (avt2Index.size() != wanted.size() + 1) {
//             throw null;
//         }
//         List<ImmutablePair<Integer, Cluster>> rr = r.stream()
//                 .map(x -> new ImmutablePair<>(avt2Index.get(x.maybePresentParent), x)).collect(Collectors.toList());
//         return rr;
//     }

//     private Comparator<? super Cluster> compareClusters() {
//         return (a, b) -> {
//             if (a.maybePresentParent == null && b.maybePresentParent == null) {
//                 return b.nodes.size() - a.nodes.size();
//             } else if (a.maybePresentParent == null) {
//                 return -1;
//             } else if (b.maybePresentParent == null) {
//                 return 1;
//             } else if (a.nodes.contains(b.maybePresentParent)
//                     || b.maybePresentParent.getParents().stream().anyMatch(x -> a.nodes.contains(x))) {
//                 return -1;
//             } else if (b.nodes.contains(a.maybePresentParent)
//                     || a.maybePresentParent.getParents().stream().anyMatch(x -> b.nodes.contains(x))) {
//                 return 1;
//             }
//             return b.nodes.size() - a.nodes.size();
//         };
//     }

//     private Comparator<? super Cluster> compareClusters2() {
//         return (a, b) -> {
//             if (a.nodes.containsAll(b.nodes)) {
//                 return -1;
//             } else if (b.nodes.containsAll(a.nodes)) {
//                 return 1;
//             }
//             return 0;
//         };
//     }

//     private void linkClusters() {
//         for (Cluster c : clusters) {
//             boolean b = true;
//             Set<Cluster> tmp = this.maybePresentNodes.get(c.maybePresentParent);
//             if (tmp == null) {
//                 Set<Cluster> tmp2 = this.maybePresentNodes.get(c.root);
//                 for (Cluster cc : tmp2) {
//                     if (c == cc || !c.nodes.stream().allMatch(x -> cc.nodes.contains(x))) {
//                         continue;
//                     }
//                     b = false;
//                     composedClusters.putIfAbsent(cc, new LinkedHashSet<>());
//                     composedClusters.get(cc).add(c);
//                 }
//                 if (b) {
//                     childClusters.putIfAbsent(null, new HashSet<>());
//                     childClusters.get(null).add(c);
//                 }
//                 continue;
//             }
//             for (Cluster pc : tmp) {
//                 if (pc.nodes.stream().anyMatch(x -> c.nodes.contains(x))) {
//                     continue;
//                 }
//                 b = false;
//                 childClusters.putIfAbsent(pc, new HashSet<>());
//                 childClusters.get(pc).add(c);
//             }
//             if (b) {
//                 childClusters.putIfAbsent(null, new HashSet<>());
//                 childClusters.get(null).add(c);
//             }
//         }
//     }

//     public boolean isInitiallyPresent(AbstractVersionedTree node) {
//         return initiallyPresentNodes.contains(node);
//     }
// }

// class Cluster {
//     AbstractVersionedTree root;
//     AbstractVersionedTree maybePresentParent;
//     LinkedHashSet<AbstractVersionedTree> nodes = new LinkedHashSet<>();
// }