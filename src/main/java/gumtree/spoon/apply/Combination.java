package gumtree.spoon.apply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Version;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import gumtree.spoon.apply.Combination.SIDE;
import gumtree.spoon.apply.Flattener.Clusterizer.Cluster;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.Operation;

public class Combination {
    static Logger logger = Logger.getLogger(Combination.class.getName());

    public static int[] rotateRight(int[] x, int n) {
        int[] result = new int[x.length];
        int j = 0;
        for (int i = x.length - n; i < x.length; i++) {
            result[j] = x[i];
            ++j;
        }
        for (int i = 0; i < x.length - n; i++) {
            result[j] = x[i];
            ++j;
        }
        return result;
    }

    static int N = 20;
    static int memoryFilled = 1;
    static int[] memory = new int[N * (N + 1) / 2];
    static {
        memory[0] = 0;
    }

    public static int[] pi(int n) {
        if (n <= 1) {
            return Arrays.copyOfRange(memory, 0, 1);
        }
        if (n <= memoryFilled) {
            return Arrays.copyOfRange(memory, n * (n - 1) / 2, n * (n + 1) / 2);
        }
        int[] result = new int[n];
        int[] rec = pi(n - 1);
        for (int i = 0; i < n - 1; i++) {
            result[i] = rec[i];
        }
        result[n - 1] = n - 1;
        int[] result2 = follow(result);
        int[] rotated = rotateRight(result2, 1);
        if (n < N) {
            int j = 0;
            for (int i = n * (n - 1) / 2; i < n * (n + 1) / 2; i++) {
                memory[i] = rotated[j];
                ++j;
            }
        }
        memoryFilled = n;
        return rotated;
    }

    private static int[] follow(int[] result) {
        int[] result2 = new int[result.length];
        for (int i = 0; i < result.length; i++) {
            result2[i] = result[result[i]];
        }
        return result2;
    }

    private static int[] followPrepend(int[] result, int value) {
        int[] result2 = new int[result.length + 1];
        for (int i = 0; i < result.length; i++) {
            result2[i + 1] = result[result[i]];
        }
        result2[0] = value;
        return result2;
    }

    public static class Reflected {

        static Combination.SIDE[] binaryToPath(Combination.BINARY[] bin) {
            Combination.SIDE[] r = new Combination.SIDE[bin.length];
            Combination.SIDE prevOneSide = Combination.SIDE.L;
            Combination.BINARY prev = Combination.BINARY.O;
            for (int i = 0; i < bin.length; i++) {
                Combination.SIDE oneSide = prev.asBool() ? prevOneSide.complement() : prevOneSide;
                switch (bin[i]) {
                    case I:
                        r[i] = oneSide;
                        break;
                    case O:
                        r[i] = oneSide.complement();
                        break;
                }
                prevOneSide = oneSide;
                prev = bin[i];
            }
            return r;
        }

        static Combination.BINARY[] pathToBinary(Combination.SIDE[] path) {
            return pathToBinary(path, path.length - 1);
        }

        static Combination.BINARY[] pathToBinary(Combination.SIDE[] path, int stopIndex) {
            Combination.BINARY[] r = new Combination.BINARY[path.length];
            Combination.SIDE prevOneSide = Combination.SIDE.L;
            Combination.BINARY prev = Combination.BINARY.O;
            for (int i = 0; i <= stopIndex; i++) {
                Combination.SIDE oneSide = prev.asBool() ? prevOneSide.complement() : prevOneSide;
                if (oneSide == path[i]) {
                    r[i] = Combination.BINARY.I;
                } else {
                    r[i] = Combination.BINARY.O;
                }
                prevOneSide = oneSide;
                prev = r[i];
            }
            return r;
        }

        static Combination.SIDE[] right(Combination.SIDE[] arr) {
            Combination.SIDE[] r = new Combination.SIDE[arr.length];
            int lastLeftIndex = lastMatchingIndex(arr, Combination.SIDE.L);
            int i = 0;
            for (; i < lastLeftIndex; i++) {
                r[i] = arr[i];
            }
            if (lastLeftIndex >= 0) {
                r[lastLeftIndex] = arr[lastLeftIndex].complement();
                i++;
            }
            for (; i < arr.length; i++) {
                r[i] = Combination.SIDE.L;
            }
            return r;
        }

        static Combination.SIDE[] left(Combination.SIDE[] arr) {
            Combination.SIDE[] r = new Combination.SIDE[arr.length];
            int lastRightIndex = lastMatchingIndex(arr, Combination.SIDE.R);
            int i = 0;
            for (; i < lastRightIndex; i++) {
                r[i] = arr[i];
            }
            if (lastRightIndex >= 0) {
                r[lastRightIndex] = arr[lastRightIndex].complement();
                i++;
            }
            for (; i < arr.length; i++) {
                r[i] = Combination.SIDE.R;
            }
            return r;
        }

        private static int lastMatchingIndex(Combination.SIDE[] arr, Combination.SIDE side) {
            int result = arr.length - 1;
            for (; result >= 0; result--) {
                if (arr[result] == side) {
                    break;
                }
            }
            return result;
        }

        static class ReflectedHelper implements CombinationHelper<Integer> {

            protected SIDE[] path;

            ReflectedHelper(BINARY[] init) {
                this.path = binaryToPath(init);
            }

            @Override
            public CHANGE<Integer> next() {
                int changedIndex = lastMatchingIndex(path, Combination.SIDE.L);
                path = right(path);
                CHANGE<Integer> r = new CHANGE<>(changedIndex == -1 ? 0 : changedIndex,
                        changedIndex == -1 ? true : pathToBinary(path, changedIndex)[changedIndex].asBool());
                return r;
            }

            @Override
            public CHANGE<Integer> prev() {
                int changedIndex = lastMatchingIndex(path, Combination.SIDE.R);
                path = left(path);
                CHANGE<Integer> r = new CHANGE<>(changedIndex == -1 ? path.length - 1 : changedIndex,
                        changedIndex == -1 ? true : pathToBinary(path, changedIndex)[changedIndex].asBool());
                return r;
            }

            @Override
            public boolean isInit() {
                throw new UnsupportedOperationException();
            }

            public int minExposant() {
                return this.path.length;
            }

            @Override
            public List<Integer> getLeafList() {
                return Arrays.asList(pathToBinary(path)).stream().map(x -> {
                    switch (x) {
                        case O:
                            return 0;
                        default:
                            return 1;
                    }
                }).collect(Collectors.toList());
            };
        }

        public static CombinationHelper<Integer> build(int[] init) {
            return new ReflectedHelper(BINARY.encode(init));
        }

    }

    static class ReflectedConstrainedHelper<T> implements CombinationHelper<T> {
        Reflected.ReflectedHelper reflectedHelper;
        int[] nextKeyPoint;
        protected int[] leafs;

        public int minExposant() {
            int r = 0;
            for (int i = 0; i < leafs.length; i++) {
                if (leafs[i] != 0) {
                    r = r + 1;
                }
            }
            return r;
        }

        public int[] getLeafs() {
            return Arrays.copyOf(leafs, leafs.length);
        }

        protected int[] deps;

        public int[] getDeps() {
            return Arrays.copyOf(deps, deps.length);
        }

        private int[] leafsIndex;
        private List<CHANGE<Integer>> interstice = null;
        private int intersticeIndex = -1;
        int[] prevKeyPoint;
        private boolean next = true;
        private CHANGE<Integer> currChangedLeaf;
        private CHANGE<Integer> currChange;
        private int[] startingPoint;
        private int onesDiff;
        private T[] nodeList;

        @Override
        public List<T> getLeafList() {
            List<T> r = new ArrayList<>();
            for (int i = 0; i < leafs.length; i++) {
                if (leafs[i] != 0) {
                    r.add(nodeList[i]);
                }
            }
            return r;
        }

        protected int[] originalInit;

        public int[] getOriginalInit() {
            return Arrays.copyOf(originalInit, originalInit.length);
        }

        @Override
        public boolean isInit() {
            if (interstice != null && intersticeIndex < interstice.size())
                return false;
            else {
                for (int i = 0; i < originalInit.length; i++) {
                    if (prevKeyPoint[i] == 0 && originalInit[i] == 0) {
                    } else if (prevKeyPoint[i] != 0 && originalInit[i] != 0) {
                    } else {
                        return false;
                    }
                }
                return true;
            }
        }

        ReflectedConstrainedHelper(int[] init, int[] leafs, int[] deps, T[] nodeList)
                throws MultipleConstraintsException {
            this.reflectedHelper = new Reflected.ReflectedHelper(
                    BINARY.encode(filterInitLeafs(propagate(init, deps, leafs), leafs)));
            this.nodeList = nodeList;
            this.originalInit = init;
            this.startingPoint = init;
            this.onesDiff = 0;
            this.leafs = leafs;
            this.deps = deps;
            this.leafsIndex = IntStream.range(0, leafs.length).filter(i -> leafs[i] != 0).toArray();
            reflectedHelper.prev();
            BINARY[] leafsPrev = Reflected.pathToBinary(reflectedHelper.path); // TODO not really the prev
            this.prevKeyPoint = new int[init.length];
            int j = leafsPrev.length - 1;
            for (int i = init.length - 1; i >= 0; i--) {
                if (leafs[i] != 0) {
                    switch (leafsPrev[j]) {
                        case I:
                            prevKeyPoint[i] = 1;
                            if (deps[i] > -1) {
                                prevKeyPoint[deps[i]]++;
                            }
                            onesDiff -= init[i] == 0 ? 1 : 0;
                            break;
                        case O:
                            onesDiff += init[i] == 0 ? 0 : 1;
                    }
                    j--;
                } else {
                    if (prevKeyPoint[i] != 0) {
                        if (prevKeyPoint[i] == 1) {
                            onesDiff -= init[i] == 0 ? 1 : 0;
                        }
                        if (deps[i] > -1) {
                            prevKeyPoint[deps[i]]++;
                        }
                    } else {
                        onesDiff += init[i] == 0 ? 0 : 1;
                    }
                }
            }
            this.currChangedLeaf = reflectedHelper.next();

            this.nextKeyPoint = Arrays.copyOf(prevKeyPoint, prevKeyPoint.length);
            if (currChangedLeaf.way) {
                fillDepsNext(leafsIndex[currChangedLeaf.content]);
            } else {
                cleanDepsNext(leafsIndex[currChangedLeaf.content]);
            }
            prevKeyPoint = nextKeyPoint;
            next();
        }

        void printChange(CHANGE<?> change) {
            System.out.print("" + change.content + " " + change.way);
        }

        private static int[] propagate(int[] init, int[] deps, int[] leafs) throws MultipleConstraintsException {
            int[] r = new int[init.length];
            int toPropagate = -1;
            for (int i = r.length - 1; i >= 0; i--) {
                if (leafs[i] == 0) {
                    if (init[i] != 0) {
                        if (r[i] > 0) {
                            r[i] = 1;
                            if (deps[i] > -1) {
                                r[deps[i]]++;
                            }
                        } else {
                            if (toPropagate == -1) {
                                toPropagate = i;
                                r[i] = 1;
                            } else {
                                throw new MultipleConstraintsException(
                                        "index " + toPropagate + " is already missing children, index " + i
                                                + " cannot also miss one at the same time");
                            }
                        }
                    }
                } else {
                    if (init[i] != 0) {
                        assert r[i] == 0;
                        r[i] = 1;
                        if (deps[i] > -1) {
                            r[deps[i]]++;
                        }
                    }

                }
            }
            if (toPropagate != -1) {
                for (int i = toPropagate + 1; i < r.length; i++) {
                    if (deps[i] == toPropagate) {
                        assert r[i] == 0;
                        if (leafs[i] == 0) {
                            r[i] = 1;
                            toPropagate = i;
                        } else {
                            break;
                        }
                    }

                }
            }
            return r;
        }

        private void fillDepsNext(int index) {
            interstice = new ArrayList<>();
            if (deps[index] == -1) {
                this.currChange = new CHANGE<>(index, true);
                intersticeIndex = 1;
            } else if (nextKeyPoint[deps[index]] > 0) {
                this.currChange = new CHANGE<>(index, true);
                intersticeIndex = 1;
            } else {
                intersticeIndex = 0;
            }
            fillDepsNextAux(index);
        }

        private void fillDepsNextAux(int index) {
            nextKeyPoint[index]++;
            if (startingPoint != null) {
                if (startingPoint[index] == 0) {
                    onesDiff++;
                } else {
                    onesDiff--;
                }
                if (onesDiff == 0) {
                    intersticeIndex = interstice != null ? interstice.size() : 1;
                    startingPoint = null;
                }
            }
            if (deps[index] == -1) {
                interstice.add(0, new CHANGE<>(index, true));
                this.currChange = interstice.remove(interstice.size() - 1);
            } else if (nextKeyPoint[deps[index]] > 0) {
                interstice.add(0, new CHANGE<>(index, true));
                this.currChange = interstice.remove(interstice.size() - 1);
                nextKeyPoint[deps[index]]++;
            } else {
                interstice.add(0, new CHANGE<>(index, true));
                fillDepsNextAux(deps[index]);
            }
        }

        private void cleanDepsNext(int index) {
            interstice = new ArrayList<>();
            if (deps[index] == -1) {
                this.currChange = new CHANGE<>(index, false);
                intersticeIndex = 1;
            } else if (nextKeyPoint[deps[index]] > 1) {
                this.currChange = new CHANGE<>(index, false);
                intersticeIndex = 1;
            } else if (nextKeyPoint[deps[index]] == 0) {
                throw new RuntimeException();
            } else {
                intersticeIndex = 0;
            }
            cleanDepsNextAux(index);
        }

        private void cleanDepsNextAux(int index) {
            nextKeyPoint[index]--;
            if (startingPoint != null) {
                if (startingPoint[index] == 0) {
                    onesDiff++;
                } else {
                    onesDiff--;
                }
                if (onesDiff == 0) {
                    intersticeIndex = interstice != null ? interstice.size() : 1;
                    startingPoint = null;
                }
            }
            if (deps[index] == -1) {
                this.currChange = new CHANGE<>(index, false);
            } else if (nextKeyPoint[deps[index]] > 1) {
                this.currChange = new CHANGE<>(index, false);
                nextKeyPoint[deps[index]]--;
            } else if (nextKeyPoint[deps[index]] == 0) {
                throw new RuntimeException();
            } else {
                interstice.add(new CHANGE<>(index, false));
                cleanDepsNextAux(deps[index]);
            }
        }

        @Override
        public CHANGE<T> next() {
            CHANGE<Integer> r = nextIndex();
            return new CHANGE<>(this.nodeList[r.content], r.way);
        }

        CHANGE<Integer> nextIndex() {
            CHANGE<Integer> r;
            if (interstice != null && intersticeIndex < interstice.size()) {
                r = interstice.get(intersticeIndex);
                this.intersticeIndex += 1;
            } else {
                r = currChange;
                this.currChangedLeaf = reflectedHelper.next();
                if (prevKeyPoint[leafsIndex[currChangedLeaf.content]] == (currChangedLeaf.way ? 1 : 0)) {
                    throw null;
                }
                this.nextKeyPoint = Arrays.copyOf(prevKeyPoint, prevKeyPoint.length);
                if (currChangedLeaf.way) {
                    fillDepsNext(leafsIndex[currChangedLeaf.content]);
                } else {
                    cleanDepsNext(leafsIndex[currChangedLeaf.content]);
                }
                this.prevKeyPoint = nextKeyPoint;
                intersticeIndex = 0;
            }
            return r;
        }

        @Override
        public CHANGE<T> prev() {
            return null;
        }
    }

    public static boolean isPresentAtVersion(AbstractVersionedTree node, Version version) {
        Version insertVersion = node.getInsertVersion();
        Version removeVersion = node.getRemoveVersion();
        if (insertVersion == null && removeVersion == null) {
            return true;
        } else if (insertVersion.partiallyCompareTo(version) == Version.COMP_RES.INFERIOR && removeVersion == null) {
            return true;
        } else if (insertVersion.partiallyCompareTo(version) == Version.COMP_RES.INFERIOR
                && removeVersion.partiallyCompareTo(version) == Version.COMP_RES.SUPERIOR) {
            return true;
        }
        return false;
    }

    public static Flattener.ComposingClusterizer flatten2(EvoStateMaintainer<?> stateMaintainer, Collection<? extends MyAction<?>> wanted) {
        // TODO initial state of combinatory computation should constructed from the initial state mentioned by evoStateMaintainer
        // as a root cluster in the contrained tree (i.e -1)

        Flattener.Clusterizer c = stateMaintainer.globalClusterizer;
        throw null;
    }

    public static Flattener.ComposingClusterizer flatten(Flattener.Clusterizer original, Collection<? extends MyAction<?>> wanted) {
        Set<AtomicAction<AbstractVersionedTree>> wanted2 = new HashSet<>();
        Map<AtomicAction<AbstractVersionedTree>, Set<ComposedAction<AbstractVersionedTree>>> composed = new HashMap<>();
        for (MyAction<?> a : wanted) {
            atomize(a, wanted2, composed);
        }
        Flattener.ComposingClusterizer flat = new Flattener.ComposingClusterizer(original,wanted2);
        Set<ComposedAction<AbstractVersionedTree>> composed2 = new HashSet<>();
        for (Set<ComposedAction<AbstractVersionedTree>> ca : composed.values()) {
            composed2.addAll(ca);
        }
        for (ComposedAction<AbstractVersionedTree> ca : composed2) {
            flat.clusterize(ca);
        }
        // flat.compute(middle);
        // List<ImmutablePair<Integer, Cluster2>> l = flat.getConstrainedTree();//flat.getResult();
        return flat;
    }

    public static ReflectedConstrainedHelper<Cluster> build(Flattener.Clusterizer flat, List<ImmutablePair<Integer, Cluster>> l)
            throws MultipleConstraintsException {
        int[] init = l.stream().map(x -> flat.getActions().contains(x.right.getRoot().getMetadata(MyScriptGenerator.DELETE_ACTION)) ? 1 : 0).mapToInt(Integer::intValue)
                .toArray();//Combination.initialState(l);
        int[] leafs = Combination.detectLeafs(l);
        Cluster[] nodes = l.stream().map(ImmutablePair::getRight).toArray(Cluster[]::new);
        int[] deps = l.stream().map(ImmutablePair::getLeft).mapToInt(Integer::intValue).toArray();
        StringBuilder log = new StringBuilder();
        log.append("Build constrained tree with:");
        log.append("\ninit=");
        log.append(Arrays.toString(init));
        log.append("\nleafs=");
        log.append(Arrays.toString(leafs));
        log.append("\ndeps=");
        log.append(Arrays.toString(deps));
        logger.info(log.toString());
        if (init.length>0) {
            return new ReflectedConstrainedHelper<Cluster>(init, leafs, deps, nodes);
        } else {
            return null;            
        }
    }

    // TODO expose non composed top level by "composing" them
    static Set<AtomicAction<AbstractVersionedTree>> atomize(MyAction<?> action,
            Set<AtomicAction<AbstractVersionedTree>> wanted2,
            Map<AtomicAction<AbstractVersionedTree>, Set<ComposedAction<AbstractVersionedTree>>> composed) {
        if (action instanceof AtomicAction) {
            wanted2.add((AtomicAction<AbstractVersionedTree>) action);
            Set<AtomicAction<AbstractVersionedTree>> r = new HashSet<>();
            r.add((AtomicAction<AbstractVersionedTree>) action);
            // for (AtomicAction<AbstractVersionedTree> a : ((AtomicAction<AbstractVersionedTree>) action)
            //         .getCompressed()) {
            //     r.addAll(atomize(a, wanted2, composed));
            // }
            return r;
        } else if (action instanceof ComposedAction) {
            Set<AtomicAction<AbstractVersionedTree>> atomicComponents = new HashSet<>();
            for (MyAction<?> myAction : ((ComposedAction<AbstractVersionedTree>) action).composed()) {
                atomicComponents.addAll(atomize(myAction, wanted2, composed));
            }
            for (AtomicAction<AbstractVersionedTree> atomicAction : atomicComponents) {
                composed.putIfAbsent(atomicAction, new HashSet<>());
                composed.get(atomicAction).add((ComposedAction<AbstractVersionedTree>) action);
            }
            return atomicComponents;
        } else {
            throw null;
        }
    }

    private static int[] filterInitLeafs(int[] init, int[] leafs) {
        return IntStream.range(0, init.length).filter(i -> leafs[i] != 0).map(i -> init[i]).toArray();
    }

    public static void main(String[] args) throws MultipleConstraintsException {
        int[] init = new int[] { 1, 1, 1, 0, 1, 0, 1 }; // { 1, 1, 1, 0, 0, 0, 1 } case not handled for now (ie. half
                                                        // way intermediate cases)
        ReflectedConstrainedHelper<Integer> h = new ReflectedConstrainedHelper<>(init,
                new int[] { 0, 0, 0, 1, 1, 1, 1 }, new int[] { -1, 0, 0, 1, 1, 2, 2 },
                new Integer[] { 0, 1, 2, 3, 4, 5, 6 });
        CHANGE<Integer> next;
        int[] curr = Arrays.copyOf(init, init.length);
        printArray(curr);
        System.out.println();
        for (int i = 0; i < 30; i++) {
            next = h.next();
            curr[next.content] = next.way ? 1 : 0;
            printArray(curr);
            System.out.println(" " + next.content + " " + next.way);
        }
    }

    private static void printArray(int[] curr) {
        for (int j : curr) {
            System.out.print("" + j + " ");
        }
    }

    public static class CHANGE<T> {
        public CHANGE(T index, boolean way) {
            this.content = index;
            this.way = way;
        }

        public final T content;
        public final boolean way;
    }

    public static interface CombinationHelper<T> {
        abstract CHANGE<T> next();

        abstract CHANGE<T> prev();

        abstract boolean isInit();

        abstract int minExposant();

        abstract List<T> getLeafList();
    }

    public static class Monotonic {

        static int[] memory = new int[] { 0, 1 };
        static int[] memoryRev = new int[] { 1, 0 };
        static int[][] memoryComb = new int[][] { { 1, 1 }, { 1 } };
        static int lastN = 1;

        private int nWanted;
        private int i = 0;
        static int TODO = -1;
        static int TRY = 0;

        Monotonic(int n) {
            this.nWanted = n;
            int neededSize = 0;
            Monotonic.memory = Arrays.copyOf(memory, neededSize);
            Monotonic.memoryRev = Arrays.copyOf(memoryRev, neededSize);
            Monotonic.memoryComb = Arrays.copyOf(memoryComb, nWanted);
            if (memory.length < neededSize) {
                int oldLen = memory.length;
                for (int i = 0; i < n; i++) {
                    Monotonic.memoryComb[i] = Arrays.copyOf(memoryComb[i], n - i);
                    for (int j = 0; j < i; j++) {
                        int[] tmp;
                        do {
                            if (j % 2 == 0) {
                                tmp = next(n, j);
                                int len = Monotonic.memoryComb[j][n - j - 1];
                                if (tmp == null) {
                                    break;
                                }
                                copy(tmp, TODO, Monotonic.memory);
                            } else {
                                tmp = next(n, j, true);
                                if (tmp == null) {
                                    break;
                                }
                                copy(tmp, TODO, Monotonic.memoryRev);
                            }
                        } while (true);
                    }
                }
            }
        }

        private void copy(int[] original, int dstOffset, int[] destination) {
            int i = 0;
            for (int j = 0; j < original.length; j++) {
                destination[dstOffset + j] = original[i];
                ++i;
            }
        }

        public int[] next(int n, int j) {
            return next(n, j, false);
        }

        private boolean v1 = true;

        public int[] next(int n, int j, boolean reverse) {
            if (n == 1 && j == 0) {
                if (!reverse) {
                    return new int[] { 0, 1 };
                } else {
                    return new int[] { 1, 0 };
                }
            } else if (j >= 0 && j < n) {
                int[] perm = pi(n - 1);
                int diag = Monotonic.memoryComb[j - 1][n - j - 1 - 1];
                int right = Monotonic.memoryComb[j][n - j - 1 - 1];
                int len = diag + right;
                Monotonic.memoryComb[j][n - j - 1] = len;
                if (!reverse) {
                    if (v1) {
                    } else {

                    }
                    return new int[] { 0, 1 };
                } else {
                    return new int[] { 1, 0 };
                }
            }
            return null;
        }

        public int[] next(int n) {
            if (i < n) {
                int[] cp = i % 2 == 0 ? next(n, i) : next(n, i, true);
                if (cp == null) {
                    ++i;
                    return next(n);
                }
                return cp;
            } else {
                return null;
            }
        }

        public int[] next() {
            if (lastN < nWanted - 1) {
                while (lastN < nWanted - 1) {
                    int[] tmp = next(lastN + 1);
                    if (tmp == null) {
                        ++lastN;
                        i = 0;
                    }
                }
                ++lastN;
            } else if (lastN < nWanted) {
                ++lastN;
            }
            return next(lastN);
        }

        public static List<int[]> useAux(int n) {
            List<int[]> r = new ArrayList<>();
            // TODO avoid redoing work as we inc i (need to keep track of 2 state by parity,
            // maybe better)
            for (int i = 0; i < n; i++) {
                if (i % 2 == 0) {
                    r.addAll(aux(n, i, false));
                } else {
                    r.addAll(aux(n, i, true));
                }
            }
            return r;
        }

        public static void print(List<int[]> in) {
            for (int[] x : in) {
                for (int y : x) {
                    System.out.print(y);
                    System.out.print(" ");
                }
                System.out.println();
            }
            System.out.println();
        }

        public static List<int[]> aux(int n, int jLen, boolean reverse) {
            List<int[]> r = new ArrayList<>();
            int[][][] diag = new int[n - 1][][];
            int[][] aaa = reverse ? new int[][] { { 1 }, { 0 } } : new int[][] { { 0 }, { 1 } };
            int[][] prevL = new int[][] {};
            int[] diagC = new int[n - 1];
            if (n > 1) {
                diag[0] = new int[][] {};
            }
            for (int j = 0; j <= jLen; j++) { // each j
                int prevC = 2;
                for (int i = 0; i < n - j; i++) { // each n
                    int currC = (j == 0 || i == 0 ? 2 : diagC[i] + prevC);
                    int rintI = 0;
                    int[][] rint = new int[currC][i + j + 1];
                    if (j == 0 && i == 0) {
                        rint = aaa;
                    }
                    int[] perm = pi(i + j);
                    int[][] prevDiag = j == 0 ? new int[][] {} : diag[i];
                    if (!reverse)
                        for (int k = 0; k < prevDiag.length; k++) { // each comb for given n and j-1
                            // new arr with prepended 1 and permutation of rest
                            rint[rintI][0] = 1;
                            for (int kk = 0; kk < j + i; kk++) {// inside a given comb
                                rint[rintI][kk + 1] = prevDiag[k][perm[kk]];
                            }
                            ++rintI;
                        }
                    for (int k = 0; k < prevL.length; k++) { //
                        // new arr with prepended 0
                        rint[rintI][0] = 0;
                        for (int kk = 0; kk < j + i; kk++) {// inside a given comb
                            rint[rintI][kk + 1] = prevL[k][kk];
                        }
                        ++rintI;
                    }
                    if (reverse)
                        for (int k = 0; k < prevDiag.length; k++) { // each comb for given n and j-1
                            rint[rintI][0] = 1;
                            for (int kk = 0; kk < j + i; kk++) {// inside a given comb
                                rint[rintI][kk + 1] = prevDiag[k][perm[kk]];
                            }
                            ++rintI;
                        }

                    if (i < n - j - 1) {
                        prevC = currC;
                        prevL = rint;
                        diag[i] = rint;
                        diagC[i] = currC;
                    } else if (j == jLen) {
                        r.addAll(Arrays.asList(rint));
                    }
                }
                prevL = new int[][] {};
            }
            return r;
        }
    }

    static enum SIDE {
        L, R;

        SIDE complement() {
            switch (this) {
                case L:
                    return R;
                case R:
                    return L;
                default:
                    throw null;
            }
        }
    }

    static enum BINARY {
        O, I;

        BINARY complement() {
            switch (this) {
                case I:
                    return O;
                case O:
                    return I;
                default:
                    throw null;
            }
        }

        boolean asBool() {
            switch (this) {
                case I:
                    return true;
                case O:
                    return false;
                default:
                    throw null;
            }
        }

        static BINARY[] encode(int... arr) {
            BINARY[] r = new BINARY[arr.length];
            for (int i = 0; i < arr.length; i++) {
                switch (arr[i]) {
                    case 0:
                        r[i] = O;
                        break;
                    case 1:
                    default:
                        r[i] = I;
                        break;
                }
            }
            return r;
        }

        static boolean[] decode(BINARY[] arr) {
            boolean[] r = new boolean[arr.length];
            for (int i = 0; i < arr.length; i++) {
                r[i] = arr[i].asBool();
            }
            return r;
        }
    }

    static final boolean AddComputedDataToEvaluateConstrainedOut = false;

    public static List<int[]> constrained(int n, int[] leafs, int[] deps) {
        Set<List<Integer>> dupli = new HashSet<>();
        List<int[]> r = new ArrayList<>();
        int leafsCount = 0;
        for (int i = 0; i < n; i++) {
            leafsCount += leafs[i];
        }
        List<int[]> mono = Monotonic.useAux(leafsCount);
        int[] prev = new int[n];
        for (int[] nrx : mono) {
            int[] x = new int[nrx.length];
            for (int j = 0; j < nrx.length; j++) {
                x[nrx.length - j - 1] = nrx[j];
            }
            int[] curr = new int[n];
            curr = populateAndFillConstr(x, curr, leafs, deps);
            for (int[] aux_r : constructByConstraints(n, prev, curr, deps)) {
                if (AddComputedDataToEvaluateConstrainedOut) {
                    int[] tmp = Arrays.copyOf(aux_r, aux_r.length + 4);
                    tmp[tmp.length - 4] = 3;
                    int diffs = 0;
                    for (int i = 0; i < n; i++) {
                        diffs += aux_r[i] == prev[i] ? 0 : 1;
                    }
                    tmp[tmp.length - 3] = diffs;
                    tmp[tmp.length - 2] = dupli.contains(Arrays.stream(aux_r).boxed().collect(Collectors.toList())) ? 1
                            : 0;
                    dupli.add(Arrays.stream(aux_r).boxed().collect(Collectors.toList()));
                    tmp[tmp.length - 1] = r.size();
                    r.add(tmp);
                } else {
                    r.add(aux_r);
                }
                prev = aux_r;
            }
            if (AddComputedDataToEvaluateConstrainedOut) {
                int[] tmp = Arrays.copyOf(curr, curr.length + 4);
                tmp[tmp.length - 4] = 2;
                int diffs = 0;
                for (int i = 0; i < n; i++) {
                    diffs += curr[i] == prev[i] ? 0 : 1;
                }
                tmp[tmp.length - 3] = diffs;
                tmp[tmp.length - 2] = dupli.contains(Arrays.stream(curr).boxed().collect(Collectors.toList())) ? 1 : 0;
                dupli.add(Arrays.stream(curr).boxed().collect(Collectors.toList()));
                tmp[tmp.length - 1] = r.size();
                r.add(tmp);
            } else {
                r.add(curr);
            }
            prev = curr;
        }

        return r;
    }

    public static List<int[]> constructByConstraints(int n, int[] prev, int[] curr, int[] deps) {
        // TODO copy? curr = list(curr)
        curr = Arrays.copyOfRange(curr, 0, curr.length);
        int[] to_change = new int[n];
        List<int[]> res = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            curr = Arrays.copyOfRange(curr, 0, curr.length);
            if (to_change[i] == 1) {
                int[] tmp = Arrays.copyOfRange(prev, 0, prev.length);
                tmp[i] = 1;
                curr[i] = 1;
                curr = fillConstr(curr, 0, deps);
                res.add(0, fillConstr(tmp, 0, deps)); // TODO perfs?
            }
            if (curr[i] == 1 && prev[i] == 0 && prev[deps[i]] == 0) {
                to_change[deps[i]] = 1;
            } else if (curr[i] == 1 && prev[i] == 0 && prev[deps[i]] == 0 && to_change[deps[i]] == 1) {
                to_change[deps[i]] = 0;
            }
        }
        return res;
    }

    public static int[] fillConstr(int[] curr, int i, int[] deps) {
        for (int k = curr.length - 1; k >= 0; k--) {
            curr[deps[k]] = curr[k] | curr[deps[k]];
        }
        return curr;
    }

    public static int[] populateAndFillConstr(int[] x, int[] curr, int[] leafs, int[] deps) {
        for (int i = curr.length - 1; i >= 0; i--) {
            if (leafs[i] == 1) {
                int j = -1;
                for (int ii = 0; ii <= i; ii++) {
                    j += leafs[ii];
                }
                curr[i] = j >= 0 ? x[j] : 0;
            }
            curr[deps[i]] = curr[i] | curr[deps[i]];
        }
        return curr;
    }

    public static <T> int[] detectLeafs(List<ImmutablePair<Integer, T>> list) {
        int[] r = new int[list.size()];
        int i = 0;
        for (ImmutablePair<Integer, ?> p : list) {
            r[i] = 1;
            if (p.left == null || p.left >= 0) {
                r[p.left] = 0;
            }
            i++;
        }
        return r;
    }

    public static int[] initialState(List<ImmutablePair<Integer, AbstractVersionedTree>> list) {
        int[] r = new int[list.size()];
        int i = 0;
        for (ImmutablePair<Integer, AbstractVersionedTree> p : list) {
            boolean isPopulated = p.right.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null;
            boolean isLabel = p.right.getMetadata("type").equals("LABEL");
            boolean isParentInit = p.left == -1 || r[p.left] > 0;
            r[i] = isPopulated && isLabel && isParentInit ? 1 : 0;
            i++;
        }
        return r;
    }

    public static int[] combSizes(int[] deps) {
        int[] sizes = new int[deps.length];
        for (int i = sizes.length; i >= 0; i--) {
            sizes[i] += 1;
            if (deps[i] != i) {
                sizes[deps[i]] *= sizes[i];
            }
        }
        return sizes;
    }

    public static int[] simplifyByRankWithFusionOfChildren(int[] deps, int[] depth, int[] sizes, int[] ranks,
            int startAtRank) {
        int[] result = new int[deps.length];
        boolean[] fusioned = new boolean[deps.length];
        for (int i = 0; i < result.length; i++) {
            // if (sizes[i] > 30 && depth[i] > 3) {// fusion condition
            if (ranks[i] >= startAtRank) {// fusion condition
                fusioned[i] = true;
                result[i] = i;
            } else if (fusioned[deps[i]]) {
                fusioned[i] = true;
                result[i] = -1;
            } else {
                result[i] = i;
            }
        }
        return result;
    }

    public static int[] simplifyByRankWithDemoting(int[] deps, int[] depth, int[] sizes, int[] ranks, int startAtRank) {
        int[] result = new int[deps.length];
        boolean[] fusioned = new boolean[deps.length];
        int newi = 0;
        for (int i = 0; i < result.length; i++) {
            // if (sizes[i] > 30 && depth[i] > 3) {// fusion condition
            if (ranks[i] >= startAtRank) {// fusion condition
                fusioned[i] = true;
                result[i] = i;
            } else if (fusioned[deps[i]]) {
                fusioned[i] = true;
                result[i] = -1;
            } else {
                result[i] = i;
            }
        }
        return result;
    }
}