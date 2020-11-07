package gumtree.spoon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.AbstractTree.FakeTree;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionInt;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import gumtree.spoon.apply.Combination;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.apply.Combination.CombinationHelper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import gumtree.spoon.diff.operations.Operation;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;

public class GrayCodeTest {

    public static class IndexedSwitch {

        public int index;
        public boolean activate;

        @Override
        public String toString() {
            return "" + activate + " " + index;
        }

    }

    public static class RefSwitch<T> {

        public T ref;
        public boolean activate;

        @Override
        public String toString() {
            return "" + activate + " " + ref;
        }

    }

    @Test
    public void testJohnson1() {
        System.out.println(johnsonGrayCode(1, 0));
        System.out.println(johnsonGrayCode(2, 0));
        System.out.println(johnsonGrayCode(3, 0));
        System.out.println(johnsonGrayCode(4, 0));
        System.out.println(johnsonGrayCode(5, 0));
    }

    @Test
    public void testJohnson2() {
        System.out.println(johnsonGrayCode(1, 1));
        System.out.println(johnsonGrayCode(2, 1));
        System.out.println(johnsonGrayCode(3, 1));
        System.out.println(johnsonGrayCode(4, 1));
        System.out.println(johnsonGrayCode(5, 1));
    }

    @Test
    public void testJohnson3() {
        System.out.println(johnsonGrayCode(1, 2));
        System.out.println(johnsonGrayCode(2, 2));
        System.out.println(johnsonGrayCode(3, 2));
        System.out.println(johnsonGrayCode(4, 2));
        System.out.println(johnsonGrayCode(5, 2));
    }

    @Test
    public void testJohnson4() {
        System.out.println(johnsonGrayCode(4, 0));
        System.out.println(johnsonGrayCode(4, 1));
        System.out.println(johnsonGrayCode(4, 2));
        System.out.println(johnsonGrayCode(4, 3));
        System.out.println(johnsonGrayCode(4, 4));
        System.out.println(johnsonGrayCode(4, 5));
        System.out.println(johnsonGrayCode(4, 6));
        System.out.println(johnsonGrayCode(4, 7));
    }

    /**
     * johnson x y = if (y mod (2*x))<x then (1,y mod (2*x)) else (0,y mod (x))
     * 
     * @param x x > 0
     * @param y
     * @return
     */
    public static IndexedSwitch johnsonGrayCode(int x, int y) {
        if (x <= 0) {
            throw new RuntimeException("x<0");
        }
        IndexedSwitch result = new IndexedSwitch();
        int i = y % (2 * x);
        if (i < x) {
            result.activate = true;
            result.index = i;
        } else {
            result.activate = false;
            result.index = i % x;
        }
        return result;
        // johnson2 x y x' = if x'> b then 1-a else a where (a,b) = johnson x y
    }

    public int johnsonGrayCodeSize(int x) {
        return 2 * x;
    }

    public boolean isModified(ITree current) {
        return current instanceof FakeTree;
    }

    static String OPERATIONS = "comb.ops";

    public List<RefSwitch<Operation<?>>> constrainedCombination(ITree current) {
        List<RefSwitch<Operation<?>>> result = new ArrayList<>();
        List<ITree> changedChildren = simpIsChanged(current);
        if (isModified(current)) {
            List<Operation<?>> ops = (List<Operation<?>>) current.getMetadata(OPERATIONS);
            for (int i = 0; i < ops.size(); i++) {
                RefSwitch<Operation<?>> r = new RefSwitch<>();
                r.activate = true;
                r.ref = ops.get(i);
                result.add(r);
                for (ITree child : changedChildren) {
                    for (int j = 0; j < changedChildren.size(); j++) {

                    }
                    result.addAll(constrainedCombination(child));// TODO
                }
            }
            for (int i = 0; i < ops.size(); i++) {
                if (i < ops.size() - 1) {
                    for (ITree child : changedChildren) {
                        result.addAll(constrainedCombination(child));// TODO
                    }
                }
                RefSwitch<Operation<?>> r = new RefSwitch<>();
                r.activate = false;
                r.ref = ops.get(i);
                result.add(r);
            }
        } else {
            for (ITree child : changedChildren) {
                result.addAll(constrainedCombination(child));// TODO
            }
        }

        return result;
    }

    public List<ITree> simpIsChanged(ITree current) {
        List<ITree> result = new ArrayList<>();
        for (ITree child : current.getChildren()) {
            if (isModified(current)) {
                result.add(current);
            } else {
                result.addAll(simpIsChanged(child));
            }
        }
        return result;
    }

    public List<RefSwitch<Operation<?>>> activations(ITree current) {
        List<RefSwitch<Operation<?>>> result = new ArrayList<>();
        List<ITree> children = current.getChildren();
        if (isModified(current)) {
            List<Operation<?>> ops = (List<Operation<?>>) current.getMetadata(OPERATIONS);
            for (int i = 0; i < ops.size(); i++) {
                RefSwitch<Operation<?>> r = new RefSwitch<>();
                r.activate = true;
                r.ref = ops.get(i);
                result.add(r);
                for (ITree child : children) {
                    result.addAll(activations(child));
                }
            }
        } else {
            for (ITree child : children) {
                result.addAll(activations(child));
            }
        }

        return result;
    }

    public class ItWithConstraints {
        private long i = 0;
        private final long[] ll;

        /**
         * 
         * @param ll constraints as a tree, ll[i] give the position of parent needed by
         *           node at index i
         */
        ItWithConstraints(long[] ll) {
            this.ll = ll;
        }

        /**
         * get the new case to validate
         * 
         * @return
         */
        public long next() {
            while (++i < (0x1L << ll.length)) { // done
                long k = 0x1L; // mask corresponding to kk
                int kk = 0x0; // index in i
                for (; k < (0x1L << ll.length);) { // while k in range
                    if ((i & k) != 0) { // current bit is true ?
                        if ((i & (0x1L << ll[kk])) == 0x0L) { // needed bit is false ?
                            break;
                        }
                    }
                    k = (long) (k << 0x1L); // go from right to left
                    ++kk; // idem
                }
                if (k >= (0x1L << ll.length)) {
                    return i;
                }
            }
            return 0;
        }
    }

    @Test
    public void testItWithConstraints1() {
        ItWithConstraints it = new ItWithConstraints(new long[] { 0, 0, 2, 0 });
        long i = 0;
        do {
            long tmp = it.next();
            System.err.println(String.format("%64s", Long.toBinaryString(i ^ tmp)).replace(" ", "0"));
            i = tmp;
        } while (i > 0x0L);
    }

    @Test
    public void testItWithConstraints2() {
        for (int x : Combination.rotateRight(new int[] { 1, 2, 3 }, 3)) {
            System.out.println(x);
        }
    }

    @Test
    public void testItWithConstraints3() {
        for (int x : Combination.pi(3)) {
            System.out.println(x);
        }
    }

    @Test
    public void testItWithConstraints4() {
        for (int x : Combination.pi(10)) {
            System.out.println(x);
        }
    }

    @Test
    public void testItWithConstraints5() {
        // Combination.Monotonic.print(Combination.Monotonic.aux(1,0,true));
        // Combination.Monotonic.print(Combination.Monotonic.aux(2,0,true));
        // Combination.Monotonic.print(Combination.Monotonic.aux(2,1,true));
        // Combination.Monotonic.print(Combination.Monotonic.aux(3,0,true));
        Combination.Monotonic.print(Combination.Monotonic.aux(3, 1, true));
        Combination.Monotonic.print(Combination.Monotonic.aux(3, 2, true));
        Combination.Monotonic.print(Combination.Monotonic.aux(4, 0, true));
        Combination.Monotonic.print(Combination.Monotonic.aux(4, 1, true));
        Combination.Monotonic.print(Combination.Monotonic.aux(4, 2, true));
        Combination.Monotonic.print(Combination.Monotonic.aux(4, 3, true));
    }

    @Test
    public void testItWithConstraints6() {
        Combination.Monotonic.print(Combination.Monotonic.useAux(2));
    }

    @Test
    public void testItWithConstraints7() {
        int[] deps = new int[] { 0, 0, 0, 2, 2, 0, 5, 5, 6, 8, 9, 10, 10, 13 };
        int[] leafs = new int[] { 0, 1, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0 };
        List<int[]> constr = Combination.constrained(deps.length, leafs, deps);
        System.out.println(constr.size());
        Combination.Monotonic.print(constr);
    }

    @Test
    public void testItWithConstraints8() {
        int[] deps = new int[] { 0, 0, 0, 2 };// , 2, 0, 5, 5, 6, 8, 9, 10, 10, 13 };
        int[] leafs = new int[] { 0, 1, 0, 1 };// , 1, 0, 0, 1, 0, 0, 0, 1, 1, 0 };
        int[] constr = Combination.populateAndFillConstr(new int[] { 1, 1 }, new int[4], leafs, deps);
        // should be 1 1 1 1
        for (int x : constr) {
            System.out.print(x);
            System.out.print(" ");
        }
        System.out.println();
    }

    // [0, 0, 0, 0] 0 False 0
    // [1, 0, 0, 0] 1 False 1
    // [1, 0, 1, 0] 1 False 2
    // [1, 0, 1, 1] 1 False 3
    // [1, 1, 1, 1] 1 False 4
    // [1, 1, 0, 0] 2 False 5
    
}