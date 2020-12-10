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

public class Flattener2 {

    private Map<AtomicAction<AbstractVersionedTree>, Set<ComposedAction<AbstractVersionedTree>>> composedActions = new HashMap<>();
    private Map<AbstractVersionedTree, AtomicAction<AbstractVersionedTree>> targetToAAction = new HashMap<>();
    private Map<Cluster2, Set<Cluster2>> composedClusters = new HashMap<>();

    public Flattener2(Set<AtomicAction<AbstractVersionedTree>> wanted) {
        this.wanted = new HashSet<>();
        for (AtomicAction<AbstractVersionedTree> aa : wanted) {
            this.wanted.add(aa);
            assert !targetToAAction.containsKey(aa.getTarget());
            targetToAAction.put(aa.getTarget(), aa);
        }
    }

    private Set<AtomicAction<AbstractVersionedTree>> wanted;

    public void compute() {
        clusters = new HashSet<>();
        maybePresentNodes = new HashMap<>();
        initiallyPresentNodes = new HashSet<>();
        prepareClusters();
        childClusters = new HashMap<>();
        prepareChildH();
    }

    private void prepareClusters() {
        for (AtomicAction<AbstractVersionedTree> aa : wanted) {
            Cluster2 c = new Cluster2();
            c.root = aa.getTarget();
            c.nodes.add(aa.getTarget());
            if (aa.getTarget().getMetadata(MyScriptGenerator.DELETE_ACTION) == aa) {
                initiallyPresentNodes.add(aa.getTarget());
                // c.initiallyPresentNodes.add(aa.getTarget());
            }
            clusters.add(c);
            assert !maybePresentNodes.containsKey(aa.getTarget()) : aa;
            HashSet<Cluster2> value = new HashSet<>();
            value.add(c);
            maybePresentNodes.put(aa.getTarget(), value);
        }
    }

    private Set<AbstractVersionedTree> initiallyPresentNodes;
    private Set<Cluster2> clusters;
    private Map<AbstractVersionedTree, Set<Cluster2>> maybePresentNodes;

    private void prepareMaybePresentParent(Cluster2 c, AbstractVersionedTree n) {
        Set<Cluster2> vals = maybePresentNodes.get(n);
        if (vals != null) {
            assert vals.size() == 1;
            for (Cluster2 val : vals) {
                c.maybePresentParent = n;
                childClusters.putIfAbsent(val, new HashSet<>());
                childClusters.get(val).add(c);
                break;
            }
        } else if (n.getParent() != null) {
            prepareMaybePresentParent(c, n.getParent());
        }
    }

    public void prepareChildH() {
        for (Entry<AbstractVersionedTree, Set<Cluster2>> entry : maybePresentNodes.entrySet()) {
            assert entry.getValue().size() == 1;
            Cluster2 c = entry.getValue().iterator().next();
            AbstractVersionedTree n = entry.getKey();
            prepareMaybePresentParent(c, n.getParent());
        }
    }

    private Map<Cluster2, Set<Cluster2>> childClusters;

    private Cluster2 compose(Cluster2 c1, Cluster2 c2) {
        Cluster2 r;
        if (c1.nodes.contains(c2.maybePresentParent)) { // could be ? c1.root.equals(c2.maybePresentParent)
            // c2 can be composed into c1
            r = new Cluster2();
            r.root = c1.root;
            r.maybePresentParent = c1.maybePresentParent;
            r.nodes.addAll(c1.nodes);
            r.nodes.addAll(c2.nodes);
        } else if (c2.nodes.contains(c1.maybePresentParent)) { // idem with c2.root.equals(c1.maybePresentParent) ?
            // c1 can be composed into c2
            r = new Cluster2();
            r.root = c2.root;
            r.maybePresentParent = c2.maybePresentParent;
            r.nodes.addAll(c2.nodes);
            r.nodes.addAll(c1.nodes);
        } else {
            AbstractVersionedTree pc1 = c1.maybePresentParent;
            AbstractVersionedTree pc2 = c2.maybePresentParent;
            if (pc1 == pc2) {
                // same constraining parent
                r = new Cluster2();
                r.root = c1.root;// new AbstractVersionedTree.FakeTree(c1.root, c2.root);
                // maybePresentNodes.putIfAbsent(r.root, new HashSet<>());
                // maybePresentNodes.get(r.root).add(r);
                r.maybePresentParent = c1.maybePresentParent;
                r.nodes.addAll(c1.nodes);
                r.nodes.addAll(c2.nodes);
            } else if (pc2 == null || (pc1 != null && isInLineage(pc2, pc1, new HashSet<>()))) {
                // c1 could be more constrained
                r = new Cluster2();
                r.root = c1.root;// for now
                r.maybePresentParent = c1.maybePresentParent;
                r.nodes.addAll(c1.nodes);
                r.nodes.addAll(c2.nodes);
            } else if (pc1 == null || isInLineage(pc1, pc2, new HashSet<>())) {
                // c2 could be more constrained
                r = new Cluster2();
                r.root = c2.root;// for now
                r.maybePresentParent = c2.maybePresentParent;
                r.nodes.addAll(c2.nodes);
                r.nodes.addAll(c1.nodes);
            } else {
                // cannot be merged (at least for now)
                // could be merged if some respective parents were merged
                // maybe store ppc1 and ppc2 along with c1 and c2 to do the merge if the possibility presents itself
                return null;
            }
        }
        // r.initiallyPresentNodes.addAll(c1.initiallyPresentNodes);
        // r.initiallyPresentNodes.addAll(c2.initiallyPresentNodes);
        return r;

    }

    private Cluster2 composeReversedLeaf(Cluster2 c1, Cluster2 toReverse) {
        // TODO
        throw null;
    }

    private Cluster2 compose(Set<Cluster2> cs) {
        Cluster2 r = new Cluster2();
        Set<AbstractVersionedTree> needed = new HashSet<>();
        for (Cluster2 c : cs) {
            if (r.root == null || r.maybePresentParent == c.root) {
                r.root = c.root;
                r.maybePresentParent = c.maybePresentParent;
            }
            needed.add(r.maybePresentParent);
            r.nodes.addAll(c.nodes);
        }
        needed.removeAll(r.nodes);
        if (needed.size() > 1) {
            throw null;
        } else if (needed.size() == 1 && !needed.contains(null)) {
            throw null;
        }
        return r;
    }

    private boolean isInLineage(AbstractVersionedTree x, AbstractVersionedTree n, Set<AbstractVersionedTree> cache) {
        if (cache.contains(n)) {
            return false;
        }
        for (Cluster2 c : maybePresentNodes.get(n)) {
            if (cache.contains(c.maybePresentParent)) {
                continue;
            } else if (c.maybePresentParent == x) {
                return true;
            } else if (isInLineage(x, c.maybePresentParent, cache)) {
                return true;
            }
        }
        cache.add(n);
        return false;
    }

    public void clusterize(ComposedAction<AbstractVersionedTree> ca) {
        // Set<AbstractVersionedTree> ts = ca.getTarget();
        // for (AbstractVersionedTree n : ts) {
        //     composedActions.putIfAbsent(n, new HashSet<>());
        //     composedActions.get(n).add(ca);
        // }
        clusterizeAux(ca);
    }

    private Set<AtomicAction<AbstractVersionedTree>> clusterizeAux(ComposedAction<AbstractVersionedTree> ca) {
        Set<AtomicAction<AbstractVersionedTree>> aas = new HashSet<>();
        Set<AbstractVersionedTree> targets = ca.getTarget();
        for (MyAction<?> a : ca.composed()) {
            if (a instanceof ComposedAction) {
                aas.addAll(clusterizeAux((ComposedAction) a));
            } else if (a instanceof AtomicAction) {
                composedActions.putIfAbsent((AtomicAction) a, new HashSet<>());
                aas.add((AtomicAction) a);
            } else {
                throw null;
            }
        }
        Set<Cluster2> mergeCandidates = new HashSet<>();
        Set<Cluster2> candidates = null;
        for (AtomicAction<AbstractVersionedTree> aa : aas) {
            assert wanted.contains(aa);
            if (composedActions.get(aa).contains(ca)) {
                return aas;
            }
            composedActions.get(aa).add(ca);
            Set<Cluster2> tmp = this.maybePresentNodes.get(aa.getTarget());
            tmp.removeIf(x -> !targets.containsAll(x.nodes));
            if (candidates == null) {
                candidates = new HashSet<>(tmp);
            } else {
                candidates.retainAll(tmp);
            }
            mergeCandidates.addAll(tmp);
        }
        Cluster2 composed;
        if (candidates.size() > 0) {
            return aas;
        } else if (mergeCandidates.size() == 2) {
            Iterator<Cluster2> it = mergeCandidates.iterator();
            composed = compose(it.next(), it.next());
        } else {
            composed = compose(mergeCandidates);
        }
        this.clusters.add(composed);
        this.composedClusters.put(composed, mergeCandidates);
        for (AbstractVersionedTree n : composed.nodes) {
            this.maybePresentNodes.get(n).add(composed);
        }
        return aas;
    }

    public Set<Cluster2> getCluster(ComposedAction<AbstractVersionedTree> ca) {
        Set<Cluster2> r = new HashSet<>();
        Set<AbstractVersionedTree> targets = ca.getTarget();
        Set<Cluster2> candidates = null;
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

    /**
     * choose clusters by combinatorial complexity
     * @return
     */
    public List<ImmutablePair<Integer, Cluster2>> getConstrainedTree(Set<Cluster2> toBreak) {
        linkClusters();
        List<Cluster2> r = new ArrayList<>();
        Map<AbstractVersionedTree, Integer> avt2Index = new HashMap<>();
        avt2Index.put(null, -1);
        Queue<Cluster2> remaining = new LinkedList<>();
        remaining.add(null);
        while (!remaining.isEmpty()) {
            Cluster2 curr = remaining.poll();
            List<Cluster2> childs = new ArrayList<>(childClusters.getOrDefault(curr, new HashSet<>()));
            childs.sort((a, b) -> b.nodes.size() - a.nodes.size());
            if (curr == null) {
                for (Cluster2 c : childs) {
                    remaining.add(c);
                }
            } else if (toBreak.contains(curr)) {
                List<Cluster2> compo = new ArrayList<>(composedClusters.getOrDefault(curr, new HashSet<>()));
                compo.sort((a, b) -> b.nodes.size() - a.nodes.size());
                remaining.addAll(compo);
            } else if (avt2Index.containsKey(curr.maybePresentParent)) {
                boolean b = true;
                for (AbstractVersionedTree n : curr.nodes) {
                    if (avt2Index.containsKey(n)) {
                        b = false;
                        break;
                    }
                    avt2Index.put(n, r.size());
                }
                if (b) {
                    r.add(curr);
                    for (Cluster2 c : childs) {
                        remaining.add(c);
                    }
                } else {
                    System.err.println();
                }
            } else {
                remaining.add(curr);
            }
        }
        if (avt2Index.size() != wanted.size() + 1) {
            throw null;
        }
        List<ImmutablePair<Integer, Cluster2>> rr = r.stream()
                .map(x -> new ImmutablePair<>(avt2Index.get(x.maybePresentParent), x)).collect(Collectors.toList());
        return rr;
    }

    private Comparator<? super Cluster2> compareClusters() {
        return (a, b) -> {
            if (a.maybePresentParent == null && b.maybePresentParent == null) {
                return b.nodes.size() - a.nodes.size();
            } else if (a.maybePresentParent == null) {
                return -1;
            } else if (b.maybePresentParent == null) {
                return 1;
            } else if (a.nodes.contains(b.maybePresentParent)
                    || b.maybePresentParent.getParents().stream().anyMatch(x -> a.nodes.contains(x))) {
                return -1;
            } else if (b.nodes.contains(a.maybePresentParent)
                    || a.maybePresentParent.getParents().stream().anyMatch(x -> b.nodes.contains(x))) {
                return 1;
            }
            return b.nodes.size() - a.nodes.size();
        };
    }

    private Comparator<? super Cluster2> compareClusters2() {
        return (a, b) -> {
            if (a.nodes.containsAll(b.nodes)) {
                return -1;
            } else if (b.nodes.containsAll(a.nodes)) {
                return 1;
            }
            return 0;
        };
    }

    private void linkClusters() {
        for (Cluster2 c : clusters) {
            boolean b = true;
            Set<Cluster2> tmp = this.maybePresentNodes.get(c.maybePresentParent);
            if (tmp == null) {
                Set<Cluster2> tmp2 = this.maybePresentNodes.get(c.root);
                for (Cluster2 cc : tmp2) {
                    if (c == cc || !c.nodes.stream().allMatch(x -> cc.nodes.contains(x))) {
                        continue;
                    }
                    b = false;
                    composedClusters.putIfAbsent(cc, new LinkedHashSet<>());
                    composedClusters.get(cc).add(c);
                }
                if (b) {
                    childClusters.putIfAbsent(null, new HashSet<>());
                    childClusters.get(null).add(c);
                }
                continue;
            }
            for (Cluster2 pc : tmp) {
                if (pc.nodes.stream().anyMatch(x -> c.nodes.contains(x))) {
                    continue;
                }
                b = false;
                childClusters.putIfAbsent(pc, new HashSet<>());
                childClusters.get(pc).add(c);
            }
            if (b) {
                childClusters.putIfAbsent(null, new HashSet<>());
                childClusters.get(null).add(c);
            }
        }
    }

    public boolean isInitiallyPresent(AbstractVersionedTree root) {
        return false;
    }
}

class Cluster2 {
    AbstractVersionedTree root;
    AbstractVersionedTree maybePresentParent;
    LinkedHashSet<AbstractVersionedTree> nodes = new LinkedHashSet<>();
    // Set<AbstractVersionedTree> initiallyPresentNodes = new HashSet<>(); // might be useful in case of nodes partially shared with other clusters but it goes against one constrain per node?
}