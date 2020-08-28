package gumtree.spoon.apply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            int[][][] diag = new int[TRY + n - 1][][];
            int[][] aaa = reverse ? new int[][] { { 1 }, { 0 } } : new int[][] { { 0 }, { 1 } };
            int[][] prevL = new int[][] {};
            int[] diagC = new int[TRY + n - 1];
            if (n > 1) {
                diag[0] = new int[][] {};
            }
            for (int j = TRY + 0; j <= TRY + jLen; j++) { // each j
                int prevC = 2;
                for (int i = TRY + 0; i < TRY + n - j; i++) { // each n
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
                            for (int kk = 0; kk < TRY + j + i; kk++) {// inside a given comb
                                rint[rintI][kk + 1] = prevDiag[k][perm[kk]];
                            }
                            ++rintI;
                        }
                    for (int k = 0; k < prevL.length; k++) { //
                        // new arr with prepended 0
                        rint[rintI][0] = 0;
                        for (int kk = 0; kk < TRY + j + i; kk++) {// inside a given comb
                            rint[rintI][kk + 1] = prevL[k][kk];
                        }
                        ++rintI;
                    }
                    if (reverse)
                        for (int k = 0; k < prevDiag.length; k++) { // each comb for given n and j-1
                            rint[rintI][0] = 1;
                            for (int kk = 0; kk < TRY + j + i; kk++) {// inside a given comb
                                rint[rintI][kk + 1] = prevDiag[k][perm[kk]];
                            }
                            ++rintI;
                        }

                    if (i < TRY + n - j - 1) {
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

    public static List<int[]> constrained(int n, int[] leafs, int[] deps) {
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
                r.add(aux_r);
                prev = aux_r;
            }
            r.add(curr);
            prev = curr;
        }

        return r;
    }

    private static List<int[]> constructByConstraints(int n, int[] prev, int[] curr, int[] deps) {
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

    private static int[] fillConstr(int[] curr, int i, int[] deps) {
        for (int k = curr.length - 1; k >= 0; k--) {
            curr[deps[k]] = curr[k] | curr[deps[k]];
        }
        return curr;
    }

    private static int[] populateAndFillConstr(int[] x, int[] curr, int[] leafs, int[] deps) {
        for (int i = curr.length - 1; i >= 0; i--) {
            if(leafs[i]==1){
                int j = -1;
                for (int ii = 0; ii < i; ii++) {
                    j += leafs[ii];
                }
                curr[i] = j>=0 ? x[j]:0;
            }
            curr[deps[i]] = curr[i] | curr[deps[i]];
        }
        return curr;
    }
}