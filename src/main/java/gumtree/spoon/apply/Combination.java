package gumtree.spoon.apply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.ITree;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import gumtree.spoon.diff.operations.Operation;

public class Combination {

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
            // TODO avoid redoing work as we inc i (need to keep track of 2 state by parity, maybe better)
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

    public static <T> List<ImmutableTriple<Integer, T, Integer>> flattenItreeToList(ITree node) {
        List<ImmutableTriple<Integer, T, Integer>> r = new ArrayList<>();

        flattenAux(r, node, null);
        return r;
    }

    // CAN MATERIALIZE List with null elements for evolutions not applicable directly
    private static final String CONSTRAINING_EVOLUTIONS = "CONSTRAINING_EVOLUTIONS";
    private static final String NON_CONSTRAINING_EVOLUTIONS = "NON_CONSTRAINING_EVOLUTIONS";
    private static final String RANKS = "RANKS";

    static <T> void flattenAux(List<ImmutableTriple<Integer, T, Integer>> r, ITree node, Integer i) {
        List<T> constrEvos = (List) node.getMetadata(CONSTRAINING_EVOLUTIONS);
        List<Integer> ranks = (List) node.getMetadata(RANKS);
        Integer[] sortedIndexByRank = ranks == null ? null : computeSortIndex(ranks);
        List<T> nonConstrEvos = (List) node.getMetadata(NON_CONSTRAINING_EVOLUTIONS);
        if (constrEvos != null && constrEvos.size() > 0) {
            for (int jj = 0; jj < constrEvos.size(); jj++) {
                int j = ranks == null ? 0 : sortedIndexByRank[jj];
                T op = constrEvos.get(j);
                int newi = r.size();
                r.add(new ImmutableTriple<Integer, T, Integer>(i, op,
                        ranks != null && j < ranks.size() ? ranks.get(j) : 0));
                for (ITree child : node.getChildren()) {
                    flattenAux(r, child, newi);
                }
                if (nonConstrEvos != null)
                    for (T op2 : nonConstrEvos) {
                        r.add(new ImmutableTriple<Integer, T, Integer>(newi, op2, 0));
                    }
            }
        } else if (nonConstrEvos != null && nonConstrEvos.size() > 0) {
            for (T op2 : nonConstrEvos) {
                r.add(new ImmutableTriple<Integer, T, Integer>(i, op2, 0));
            }
            for (ITree child : node.getChildren()) {
                flattenAux(r, child, i);
            }
        } else {
            for (ITree child : node.getChildren()) {
                flattenAux(r, child, i);
            }
        }
    }

    // HELPERS

    private static Integer[] computeSortIndex(List<Integer> ranks) {
        Integer[] result = new Integer[ranks.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = i; // Autoboxing
        }
        Arrays.sort(result, new Comparator<Integer>() {

            @Override
            public int compare(Integer arg0, Integer arg1) {
                return ranks.get(arg0).compareTo(ranks.get(arg1));
            }

        });
        return result;
    }

    public static <T> int[] extractDeps(List<ImmutableTriple<Integer, T, Integer>> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i).left;
        }
        return result;
    }

    public static <T> int[] extractRanks(List<ImmutableTriple<Integer, T, Integer>> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i).right;
        }
        return result;
    }

    public static <T> List<T> extractOps(List<ImmutableTriple<Integer, T, Integer>> list) {
        return list.stream().map(x -> x.middle).collect(Collectors.toList());
    }

    public static int[] extractLeafs(int[] deps) {
        boolean[] isDeps = new boolean[deps.length];
        int[] result = new int[deps.length];
        for (int i = result.length; i >= 0; i--) {
            if (!isDeps[i]) {
                result[i] = 1;
            }
            if (deps[i] != i) {
                isDeps[deps[i]] = true;
            }
        }
        return result;
    }

    public static int[] extractDepth(int[] deps) {
        int[] result = new int[deps.length];
        for (int i = 0; i < result.length; i++) {
            if (deps[i] == i) {
                result[i] = 0;
            } else {
                result[i] = result[deps[i]] + 1;
            }
        }
        return result;
    }

    public static int combSize(int[] deps) {
        int total = 1;
        int[] sizes = new int[deps.length];
        for (int i = sizes.length; i >= 0; i--) {
            sizes[i] += 1;
            if (deps[i] != i) {
                sizes[deps[i]] *= sizes[i];
            } else {
                total *= sizes[i];
            }
        }
        return total;
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

    public static int[] simplifyByRankWithDemoting(int[] deps, int[] depth, int[] sizes, int[] ranks,
            int startAtRank) {
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