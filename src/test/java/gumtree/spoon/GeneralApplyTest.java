package gumtree.spoon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionInt;

import org.junit.Test;

import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.MultiDiffImpl;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.support.SpoonSupport;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.VirtualFile;

public class GeneralApplyTest {
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

    @Test
    public void test3() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { void f(){} };", "X.java");
        CtPackage rp1 = MyUtils.makePkg(r1);

        VirtualFile r2 = new VirtualFile("class Y { class X { void f(){} } };", "Y.java");
        CtPackage rp2 = MyUtils.makePkg(r2);

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
        CtPackage rp1 = MyUtils.makePkg(r1, new VirtualFile("class BBB {};", "BBB.java"));

        VirtualFile r2 = new VirtualFile("class BBB { class X { class Z { void f(){} } } };", "BBB.java");
        CtPackage rp2 = MyUtils.makePkg(r2, new VirtualFile("class AAA {};", "AAA.java"));

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
        CtPackage rp1 = MyUtils.makePkg(r1);

        VirtualFile r2 = new VirtualFile("class X { class Y { class Z { void f(){} } } void g(){} };", "X.java");
        CtPackage rp2 = MyUtils.makePkg(r2);

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
        CtPackage rp1 = MyUtils.makePkg(r1);

        VirtualFile r2 = new VirtualFile("class X { class Y { class Z { void g(){} } }  };", "X.java");
        CtPackage rp2 = MyUtils.makePkg(r2);

        AstComparator diff = new AstComparator();

        Diff editScript = diff.compare(rp1, rp2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
            System.out.println(x.toJson());
        }
        System.out.println();
    }

    @Test
    public void test6_1() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { int a; int b; int c; void g(){ a + b;} int f(int x){ return x;} }",
                "X.java");
        CtPackage rp1 = MyUtils.makePkg(r1);

        VirtualFile r2 = new VirtualFile(
                "class X { int a; int b; int c; void g(){ a + f(c+b);} int f(int x){ return x;} }", "X.java");
        CtPackage rp2 = MyUtils.makePkg(r2);

        AstComparator diff = new AstComparator();

        Diff editScript = diff.compare(rp1, rp2);
        for (Operation<?> x : editScript.getAllOperations()) {
            System.out.println(x.toString());
        }
        System.out.println();
    }

    @Test
    public void test7() {
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { void f() {} class Y {} };", "X.java");
        CtPackage rp1 = MyUtils.makePkg(r1);
        VirtualFile r2 = new VirtualFile("class AAA { void g() {} };", "AAA.java");
        CtPackage rp2 = MyUtils.makePkg(r2);

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

    @Test
    public void testApply1() {
        // System.setProperty("gumtree.match.gt.ag.nomove", "true");
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile("class X { static void f() {} class Y { h () { X.f(); } } };", "X.java");
        Factory left = MyUtils.makeFactory(r1);
        VirtualFile r2 = new VirtualFile("class AAA { void g() {} class Y { h () { X.f(); } } };", "AAA.java");
        VirtualFile r1b = new VirtualFile("class X { static void f() {} };", "X.java");
        Factory right = MyUtils.makeFactory(r1b, r2);
        VirtualFile r3 = new VirtualFile("class AAA { void g() {} };", "AAA.java");
        VirtualFile r3bb = new VirtualFile("class BBB { class Y { h () { X.f(); } } };", "BBB.java");
        VirtualFile r3b = new VirtualFile("class X { static void f() {} };", "X.java");
        Factory right2 = MyUtils.makeFactory(r3b, r3, r3bb);

        final Version leftV = new VersionInt(0);
        final Version rightV = new VersionInt(1);

        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        ITree srctree = scanner.getTree(left.getModel().getRootPackage());
        MultiDiffImpl mdiff = new MultiDiffImpl(srctree, leftV);
        Diff diff = mdiff.compute(scanner.getTree(right.getModel().getRootPackage()), rightV);
        System.out.println(scanner.getTree(left.getModel().getRootPackage().clone()).toShortString());
        CtAbstractInvocation<?> invo = (CtAbstractInvocation<?>) right.getModel().getRootPackage().getType("AAA")
                .getNestedType("Y").getMethod("h").getBody().getStatements().get(0);
        System.out.println(invo.getExecutable().getDeclaration());
        System.out.println(left.getModel().getRootPackage().clone().prettyprint()
                .equals(left.getModel().getRootPackage().prettyprint()));
        for (Operation<?> x : diff.getAllOperations()) {
            System.out.println(x.getClass());
            if (x instanceof InsertOperation) {
                System.out.println("------");
                System.out.println(((InsertOperation) x).getSrc());
                System.out.println(((InsertOperation) x).getSrc().getClass());
                System.out.println("++++++");
                System.out.println(((InsertOperation) x).getDst());
            } else if (x instanceof DeleteOperation) {
                System.out.println("---====---");
                System.out.println(((DeleteOperation) x).getSrc());
                System.out.println(((DeleteOperation) x).getSrc().getClass());
                System.out.println("+++====+++");
                System.out.println(((DeleteOperation) x).getDst());
            } else {

            }
        }
        final Version rightV2 = new VersionInt(2);
        ITree qqq = mdiff.getMiddle();
        System.out.println(qqq);
        System.out.println("--------------");
        System.out.println(qqq.toTreeString());
        ITree afegwsegwse = scanner.getTree(right2.getModel().getRootPackage());
        System.out.println(afegwsegwse.toTreeString());
        Diff diff2 = mdiff.compute(afegwsegwse, rightV2);
        ITree qqq2 = mdiff.getMiddle();
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), qqq));
    }

    @Test
    public void testApply2() {
        // System.setProperty("gumtree.match.gt.ag.nomove", "true");
        // contract: toString should be able to print move of a toplevel class
        VirtualFile r1 = new VirtualFile(
                "class X { static void f() {} class Y { h () { if(true)X.f();else X.f(); } } };", "X.java");
        Factory left = MyUtils.makeFactory(r1);
        VirtualFile r2 = new VirtualFile(
                "class AAA { void g() {} class Y { h () { if(true)X.f();else if (false) X.f(); } } };", "AAA.java");
        VirtualFile r1b = new VirtualFile("class X { static void f() {} };", "X.java");
        Factory right = MyUtils.makeFactory(r1b, r2);

        final Version leftV = new VersionInt(0);
        final Version rightV = new VersionInt(1);

        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        ITree srctree = scanner.getTree(left.getModel().getRootPackage());
        MultiDiffImpl mdiff = new MultiDiffImpl(srctree,leftV);
        Diff diff = mdiff.compute(scanner.getTree(right.getModel().getRootPackage()), rightV);
        System.out.println(scanner.getTree(left.getModel().getRootPackage().clone()).toShortString());
        for (Operation<?> x : diff.getAllOperations()) {
            System.out.println(x.getClass());
            if (x instanceof InsertOperation) {
                System.out.println("------");
                System.out.println(((InsertOperation) x).getSrc());
                System.out.println(((InsertOperation) x).getSrc().getClass());
                System.out.println("++++++");
                System.out.println(((InsertOperation) x).getDst());
            } else if (x instanceof DeleteOperation) {
                System.out.println("---====---");
                System.out.println(((DeleteOperation) x).getSrc());
                System.out.println(((DeleteOperation) x).getSrc().getClass());
                System.out.println("+++====+++");
                System.out.println(((DeleteOperation) x).getDst());
            } else {

            }
        }
        ITree qqq = mdiff.getMiddle();
        System.out.println(qqq);
        System.out.println("--------------");
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), qqq));
    }
}