package gumtree.spoon;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.AbstractTree.FakeTree;

import org.junit.Test;

import gumtree.spoon.apply.Combination;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import gumtree.spoon.diff.support.SpoonSupport;
import spoon.SpoonModelBuilder;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

public class MyTest {
    @Test
    public void testRemove() {
        String c1 = "" + "class X {" + "public void foo0() {" + " int x = 0;return;" + "}" + "};";

        String c2 = "" + "class X {" + "public void foo0() {" + " int x = 0;" + "}" + "};";

        AstComparator diff = new AstComparator();
        Diff editScript = diff.compare(c1, c2);
        assertEquals(1, editScript.getRootOperations().size());

        Operation op = editScript.getRootOperations().get(0);
        assertTrue(op instanceof DeleteOperation);

        assertNotNull(op.getSrcNode());
        assertEquals("return", op.getSrcNode().toString());

        CtMethod methodSrc = op.getSrcNode().getParent(CtMethod.class);
        assertEquals(2, methodSrc.getBody().getStatements().size());
        assertNotNull(methodSrc);

        SpoonSupport support = new SpoonSupport();
        CtMethod methodTgt = (CtMethod) support.getMappedElement(editScript, methodSrc, true);
        assertNotNull(methodTgt);
        assertEquals("foo0", methodTgt.getSimpleName());
        assertEquals(1, methodTgt.getBody().getStatements().size());
        assertEquals("int x = 0", methodTgt.getBody().getStatements().get(0).toString());
    }

    @Test
    public void test1() {
        String c1 = "class X { class Y {} class Z { void f(){} } };";

        String c2 = "class X { class Y { class Z { void f(){} } } };";

        AstComparator diff = new AstComparator();
        Diff editScript = diff.compare(c1, c2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
            System.out.println(x.toJson());
        }
        System.out.println();
    }

    @Test
    public void test2() {
        String c1 = "class X {};";

        String c2 = "class Y {};";

        AstComparator diff = new AstComparator();
        Diff editScript = diff.compare(c1, c2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
            System.out.println(x.toJson());
        }
        System.out.println();
    }

    protected Factory createFactory() {
        Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
        factory.getEnvironment().setNoClasspath(true);
        factory.getEnvironment().setCommentEnabled(false);
        return factory;
    }

    @Test
    public void test3() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { void f(){} };", "X.java");
        CtPackage rp1 = extracted(r1);

        VirtualFile r2 = new VirtualFile("class Y { class X { void f(){} } };", "Y.java");
        CtPackage rp2 = extracted(r2);

        AstComparator diff = new AstComparator();

        Diff editScript = diff.compare(rp1, rp2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
            System.out.println(x.toJson());
        }
        System.out.println();
    }

    @Test
    public void test4() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class AAA { class Y { class Z { void f(){} } } };", "AAA.java");
        CtPackage rp1 = extracted(r1, new VirtualFile("class BBB {};", "BBB.java"));

        VirtualFile r2 = new VirtualFile("class BBB { class X { class Z { void f(){} } } };", "BBB.java");
        CtPackage rp2 = extracted(r2, new VirtualFile("class AAA {};", "AAA.java"));

        AstComparator diff = new AstComparator();

        Diff editScript = diff.compare(rp1, rp2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
            System.out.println("----parent----");
            if (x.getAction() instanceof Addition) {
                System.out.println(((Addition) x.getAction()).getParent().toString());
            }
            System.out.println("----src----");
            if (x.getSrcNode() != null) {
                System.out.println("----src-parent----");
                if (x.getSrcNode().getParent() != null) {
                    System.out.println(x.getSrcNode().getParent().toString());
                }
                System.out.println("--------");
                System.out.println(x.getSrcNode().toString());
            }
            System.out.println("----dst----");
            if (x.getDstNode() != null) {
                System.out.println("----dst-parent----");
                if (x.getDstNode().getParent() != null) {
                    System.out.println(x.getDstNode().getParent().toString());
                }
                System.out.println("--------");
                System.out.println(x.getDstNode().toString());
            }
            System.out.println(x.toJson());
        }
        System.out.println();
    }

    @Test
    public void test5() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { class Y {} class Z { void f(){} } void g(){} };", "X.java");
        CtPackage rp1 = extracted(r1);

        VirtualFile r2 = new VirtualFile("class X { class Y { class Z { void f(){} } } void g(){} };", "X.java");
        CtPackage rp2 = extracted(r2);

        AstComparator diff = new AstComparator();

        Diff editScript = diff.compare(rp1, rp2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
            System.out.println(x.toJson());
        }
        System.out.println();
    }

    @Test
    public void test6() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { class Y {} class Z { void f(){} } void g(){} };", "X.java");
        CtPackage rp1 = extracted(r1);

        VirtualFile r2 = new VirtualFile("class X { class Y { class Z { void g(){} } }  };", "X.java");
        CtPackage rp2 = extracted(r2);

        AstComparator diff = new AstComparator();

        Diff editScript = diff.compare(rp1, rp2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
            System.out.println(x.toJson());
        }
        System.out.println();
    }

    @Test
    public void test7() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { void f() {} class Y {} };", "X.java");
        CtPackage rp1 = extracted(r1);
        VirtualFile r2 = new VirtualFile("class AAA { void g() {} };", "AAA.java");
        CtPackage rp2 = extracted(r2);

        rp1.getType("X").addTypeMemberAt(3, rp2.getType("AAA"));
        rp1.getType("X").addTypeMemberAt(3, rp2.getType("AAA"));
        // rp1.getType("X").getDirectChildren().get(rp1.getType("X").getDirectChildren().size()-1).replace((CtElement)rp2.getType("AAA"));
        // rp1.getType("X").getDirectChildren().get(0).replace((CtElement)rp2.getType("AAA"));
        for (CtType<?> type : rp1.getTypes()) {
            for (CtElement child : type.getDirectChildren()) {
                System.out.println("-----------");
                System.out.println(child);
            }
        }
        System.out.println("==========");
        System.out.println(rp1.getTypes());
        for (CtType<?> type : rp1.getTypes()) {
            System.out.println(type.getDirectChildren().size());
        }
    }

    private CtPackage extracted(VirtualFile... resources) {
        Factory factory = createFactory();
        factory.getModel().setBuildModelIsFinished(false);
        SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
        compiler.getFactory().getEnvironment().setLevel("OFF");
        for (VirtualFile resource : resources) {
            compiler.addInputSource(resource);
        }
        compiler.build();
        CtPackage rp = factory.getModel().getRootPackage();
        return rp;
    }

    public class IndexedSwitch {

        public int index;
        public boolean activate;

        @Override
        public String toString() {
            return "" + activate + " " + index;
        }

    }

    public class RefSwitch<T> {

        public T ref;
        public boolean activate;

        @Override
        public String toString() {
            return "" + activate + " " + ref;
        }

    }

    /**
     * johnson x y = if (y mod (2*x))<x then (1,y mod (2*x)) else (0,y mod (x))
     * @param x x > 0
     * @param y
     * @return
     */
    public IndexedSwitch johnsonGrayCode(int x, int y) {
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
         * @param ll constraints as a tree, ll[i] give the position of parent needed by node at index i
         */
        ItWithConstraints(long[] ll) {
            this.ll = ll;
        }

        /**
         * get the new case to validate
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
        Combination.Monotonic.print(Combination.Monotonic.useAux(5));
    }

    @Test
    public void testItWithConstraints7() {
        int[] deps = new int[] { 0, 0, 0, 2, 2, 0, 5, 5, 6, 8, 9, 10, 10, 13 };
        int[] leafs = new int[] { 0, 1, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0 };
        Combination.Monotonic.print(Combination.constrained(14, leafs, deps));
    }

}