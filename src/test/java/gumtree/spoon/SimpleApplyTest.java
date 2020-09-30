package gumtree.spoon;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import javax.lang.model.util.ElementScanner6;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractTree;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.VersionedTree;
import com.github.gumtreediff.tree.AbstractTree.FakeTree;

import org.apache.commons.lang3.tuple.MutablePair;
import org.junit.Before;
import org.junit.Test;

import gumtree.spoon.apply.AAction;
import gumtree.spoon.apply.Combination;
import gumtree.spoon.apply.operations.MyCloneHelper;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import gumtree.spoon.diff.operations.*;
import gumtree.spoon.diff.support.SpoonSupport;
import spoon.ContractVerifier;
import spoon.MavenLauncher;
import spoon.SpoonModelBuilder;
import spoon.compiler.Environment;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.compiler.Environment.PRETTY_PRINTING_MODE;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtRHSReceiver;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.reflect.CtExtendedModifier;
import gumtree.spoon.MyUtils;

public class SimpleApplyTest {
    @Before
    public void initProps() {
        System.setProperty("nolabel", "true");
    }

    public static class applyTestHelper {
        public static void onInsert(CtModel right) {
            final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
            CtModel left = MyUtils.makeFactory().getModel();
            ITree srctree = scanner.getTree(left.getRootPackage());
            MultiDiffImpl mdiff = new MultiDiffImpl(srctree);

            ITree dstTree = scanner.getTree(right.getRootPackage());
            DiffImpl diff = mdiff.compute(scanner.getTreeContext(), dstTree);

            ITree middle = mdiff.getMiddle();
            Environment env = new StandardEnvironment();
            spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);
            CtRootPackage middleRP = (CtRootPackage) middle.getChild(0).getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

            for (Action action : diff.getActionsList()) {
                applyAInsert(middleRP.getFactory(), scanner.getTreeContext(), (Insert & AAction<Insert>) action);
            }

            assertEquals(pp.prettyprint(right.getAllTypes().iterator().next()),
                    pp.prettyprint(middleRP.getTypes().iterator().next()));
        }
    }

    public void auxInsTestHelper(CtElement right) {
        // ContractVerifier cv = new
        // spoon.ContractVerifier(right.getFactory().getModel().getRootPackage());
        // try {
        // cv.verify();
        // } catch (AssertionError e) {
        // assumeNoException(e);
        // }
        Environment env = new StandardEnvironment();
        spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);
        // System.err.println(pp.prettyprint(right));
        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        CtElement left = null;// MyUtils.makeFactory().getModel().getRootPackage();
        ITree srctree;
        srctree = scanner.getTree(left);
        MultiDiffImpl mdiff = new MultiDiffImpl(srctree);
        ITree dstTree = scanner.getTree(right);
        DiffImpl diff = mdiff.compute(scanner.getTreeContext(), dstTree);

        ITree middle = mdiff.getMiddle();
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), middle));
        for (Action action : diff.getActionsList()) {
            SimpleApplyTest.applyAInsert((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                    (Insert & AAction<Insert>) action);
        }
        CtElement middleE = null;
        Queue<ITree> tmp = new LinkedBlockingDeque<>();
        tmp.add(middle);

        while (middleE == null) {
            ITree curr = tmp.poll();
            middleE = (CtElement) curr.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            // if (middleE instanceof CtRootPackage) {
            //     middleE = ((CtRootPackage) middleE).getTypes().iterator().next();
            // }
            List<ITree> children = curr.getChildren();
            tmp.addAll(children);
        }
        // .getModel().getRootPackage().getTypes().iterator().next()
        HashMap<String, MutablePair<CtType, CtType>> res = new HashMap<>();
        compare(pp, right, middleE, res);
        System.err.println("00000000000000000");
        System.err.println(res.values().size());
        for (MutablePair<CtType, CtType> p : res.values()) {
            System.err.println(p.right.getClass());
            System.err.println("*" + p.right.getQualifiedName() + "*");
            try {
                System.err.println(pp.prettyprint(p.left));
            } catch (Exception e) {
                assumeNoException(e);
            }
            // try {
            //     System.err.println(pp.prettyprint(p.right));
            // } catch (Exception e) {
            // }
        }
        System.err.println("00000000000000000");
        for (MutablePair<CtType, CtType> p : res.values()) {
            assertEquals(pp.prettyprint(p.left), pp.prettyprint(p.right));
        }
    }

    private void compare(PrettyPrinter pp, CtElement right, CtElement middle,
            Map<String, MutablePair<CtType, CtType>> res) {
        System.err.println("1111111111111111111111");
        System.err.println(right.getClass());
        if (right instanceof CtType) {
            assertTrue(middle instanceof CtType);
            res.put(((CtType) right).getQualifiedName(), new MutablePair(right, middle));
        } else if (right instanceof CtPackage) {
            System.err.println("222222222222222222");
            assertTrue(middle.getClass().toString(), middle instanceof CtPackage);
            Map<String, MutablePair<CtType, CtType>> m = new HashMap<>();
            for (CtType<?> t : ((CtPackage) right).getTypes()) {
                m.put(t.getQualifiedName(), new MutablePair(t, null));
            }
            for (CtType<?> t : ((CtPackage) middle).getTypes()) {
                m.get(t.getQualifiedName()).setRight(t);
            }
            for (MutablePair<CtType, CtType> p : m.values()) {
                assertNotNull(p.left);
                assertNotNull(p.right);
                compare(pp, p.left, p.right, res);
            }
            Map<String, MutablePair<CtPackage, CtPackage>> m2 = new HashMap<>();
            for (CtPackage t : ((CtPackage) right).getPackages()) {
                m2.put(t.getQualifiedName(), new MutablePair(t, null));
            }
            for (CtPackage t : ((CtPackage) middle).getPackages()) {
                m2.get(t.getQualifiedName()).setRight(t);
            }
            for (MutablePair<CtPackage, CtPackage> p : m2.values()) {
                assertNotNull(p.left);
                assertNotNull(p.right);
                compare(pp, p.left, p.right, res);
            }
        }
    }

    private void insertTestHelper(String contents) {
        // final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        // Factory left = MyUtils.makeFactory();
        Factory right = MyUtils.makeFactory(new VirtualFile(contents, "X.java"));
        auxInsTestHelper(right.getModel().getRootPackage());
        // ContractVerifier cv = new spoon.ContractVerifier(right.getModel().getRootPackage());

        // cv.checkShadow();
        // // cv.checkParentContract();
        // // cv.checkParentConsistency();
        // cv.checkModifiers();
        // cv.checkAssignmentContracts();
        // cv.checkContractCtScanner();
        // cv.checkBoundAndUnboundTypeReference();
        // cv.checkModelIsTree();
        // cv.checkContractCtScanner();
        // cv.checkElementIsContainedInAttributeOfItsParent();
        // cv.checkElementToPathToElementEquivalence();
        // cv.checkRoleInParent();
        // cv.checkJavaIdentifiers();

        // ITree srctree = scanner.getTree(left.getModel().getRootPackage());
        // MultiDiffImpl mdiff = new MultiDiffImpl(srctree);

        // ITree dstTree = scanner.getTree(right.getModel().getRootPackage());
        // DiffImpl diff = mdiff.compute(scanner.getTreeContext(), dstTree);

        // List<Operation> ops = diff.getAllOperations();
        // for (Operation<?> op : ops) {
        //     Action action = op.getAction();
        //     System.out.println(action.format(scanner.getTreeContext()));
        //     if (action instanceof AAction) {
        //         AAction aaction = (AAction) action;
        //         ITree leftNode = aaction.getSource();
        //         System.out.println("\t" + MyUtils.toPrettyString(scanner.getTreeContext(), leftNode));
        //         ITree rightNode = aaction.getTarget();
        //         System.out.println("\t" + MyUtils.toPrettyString(scanner.getTreeContext(), rightNode));
        //         if (aaction instanceof Update) {
        //             System.out.println("\t" + leftNode.getMetadata("type") + "\t"
        //                     + ((AbstractVersionedTree) leftNode).getAllChildren().size());
        //             System.out.println("\t" + rightNode.getMetadata("type") + "\t"
        //                     + ((AbstractVersionedTree) rightNode).getAllChildren().size());
        //         }
        //     }
        // }

        // System.out.println(".......0........");
        // ITree middle = mdiff.getMiddle();
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), middle));
        // System.out.println(".......1........");

        // CtRootPackage middleRP = (CtRootPackage) middle.getChild(0).getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        // System.out.println(middleRP);
        // System.out.println(diff.getActionsList());
        // System.out.println("........2.......");
        // for (Action action : diff.getActionsList()) {
        //     applyAInsert(middleRP.getFactory(), scanner.getTreeContext(), (Insert & AAction<Insert>) action);
        // }
        // System.out.println(".......3........");

        // // PrettyPrinter pp =
        // // middleRP.getFactory().getEnvironment().createPrettyPrinter(); // TODO HARDER
        // PrettyPrinter pp = middleRP.getFactory().getEnvironment().createPrettyPrinterAutoImport();
        // System.out.println("===expected===");
        // for (CtType<?> type : right.getModel().getAllTypes()) {
        //     System.out.println("......." + type.getPosition().getFile().getAbsolutePath() + "........");
        //     System.out.println(pp.prettyprint(type));
        // }
        // System.out.println("===given===");
        // for (CtType<?> type : middleRP.getTypes()) {
        //     type.getPosition().getCompilationUnit();
        //     System.out.println("......." + type.getPosition().getFile().getAbsolutePath() + "........");
        //     System.out.println(pp.prettyprint(type));
        // }
        // assertEquals(pp.prettyprint(right.getModel().getAllTypes().iterator().next()),
        //         pp.prettyprint(middleRP.getTypes().iterator().next()));
        // System.out.println(".......4........");
    }

    @Test
    public void testSimpleApplyInsertInterface() {
        String contents = "interface X {}";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceMeth() {
        String contents = "interface X { static void f(){} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceMethDecl() {
        String contents = "interface X { void f(); }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldInteger() {
        String contents = "interface X { int value = 0; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldStringNum() {
        String contents = "interface X { String value = \"54246\"; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldString2() {
        String contents = "interface X { String value = \"\\u0087\"; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldString() {
        String contents = "interface X { String value = \"\"; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldBooleanTrue() {
        String contents = "interface X { boolean value = true; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldBooleanFalse() {
        String contents = "interface X { boolean value = false; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldNull() {
        String contents = "interface X { String value = null; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldChar() {
        String contents = "interface X { char value = 'c'; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldFloat() {
        String contents = "interface X { float value = 0.1f; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldLong() {
        String contents = "interface X { long value = 0l; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldDouble() {
        String contents = "interface X { double value = 0.0d; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceField1PLUS1() {
        String contents = "interface X { int value = 1+1; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceField1PLUS2() {
        String contents = "interface X { int value = 1+2; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldInc() {
        String contents = "interface X { int value = 1++; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldsNeg() {
        String contents = "interface X { int value = 1; int value2 = -value; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldsNegFull() {
        String contents = "interface X { int value = 1; int value2 = -X.value; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterface2() {
        String contents = "package org; interface X {}";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceMethEmptRet() {
        String contents = "interface X { static void f(){ return;} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClass() {
        String contents = "class X {}";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassFieldsInc() {
        String contents = "class X { static int value = 1; static int value2 = value++; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassFieldsStaticConstr() {
        String contents = "class X { static int value; static {value = 1;} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassFieldsStaticConstrInc() {
        String contents = "public class X { static int value = 0; static {value += 1;} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRet1() {
        String contents = "class X { int f() {return 1;} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRet1Plus1() {
        String contents = "class X { int f() {return 1 + 1;} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRec() {
        String contents = "class X { int f() {return f();} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRec2() {
        String contents = "class X { int f() {return X.this.f();} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodParam() {
        String contents = "class X { void f(int i) {} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodParam2() {
        String contents = "class X { void f(int i, int j) {} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodParam2X() {
        String contents = "class X { void f(X i, X j) {} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassDoubleMethod() {
        String contents = "class X { int add(int i) { return i + i; } }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassAdditionMethod() {
        String contents = "class X { int add(int i, int j) { return i + j; } }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassPrintMethod() {
        String contents = "class X { void print(String s) { System.out.println(s); } }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassInterface() {
        String contents = "class X { interface Y {} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClassInterface5() {
        String contents = "interface A {public abstract synchronized strictfp volatile transient final void f();}";
        insertTestHelper(contents);
    }

    public static void applyAInsert(Factory factory, TreeContext ctx, AAction<Insert> action) {
        ITree source = action.getSource();
        AbstractVersionedTree target = action.getTarget();
        AbstractVersionedTree parentTarget = target.getParent();
        System.out.println("=======");
        System.out.println(MyUtils.toPrettyString(ctx, source));
        System.out.println(MyUtils.toPrettyString(ctx, target));
        System.out.println(MyUtils.toPrettyString(ctx, parentTarget));
        String targetType = (String) target.getMetadata("type");
        switch (targetType) {
            case "LABEL": {
                System.out.println("isLabel");
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent == null) {
                    System.err.println(target);
                    System.err.println(parentTarget);
                } else if (parent instanceof CtNamedElement) {
                    ((CtNamedElement) parent).setSimpleName(target.getLabel());
                } else if (parent instanceof CtTypeReference) {
                    ((CtTypeReference<?>) parent).setSimpleName(target.getLabel());
                } else if (parent instanceof CtBinaryOperator) {
                    ((CtBinaryOperator<?>) parent).setKind(MyUtils.getBinaryOperatorByName(target.getLabel()));
                } else if (parent instanceof CtUnaryOperator) {
                    ((CtUnaryOperator<?>) parent).setKind(MyUtils.getUnaryOperatorByName(target.getLabel()));
                } else if (parent instanceof CtLiteral) {
                    if (target.getLabel().startsWith("\"")) {
                        ((CtLiteral<Object>) parent).setValue(
                                target.getLabel().substring(1, target.getLabel().length() - 1).replace("\\\\", "\\"));
                    } else if (target.getLabel().startsWith("'")) {
                        ((CtLiteral<Character>) parent)
                                .setValue(target.getLabel().substring(1, target.getLabel().length() - 1).charAt(0));
                    } else if (target.getLabel().equals("true")) {
                        ((CtLiteral<Object>) parent).setValue(true);
                    } else if (target.getLabel().equals("false")) {
                        ((CtLiteral<Object>) parent).setValue(false);
                    } else if (target.getLabel().equals("null")) {
                        ((CtLiteral<Object>) parent).setValue(null);
                    } else if (target.getLabel().endsWith("F")) {
                        ((CtLiteral<Float>) parent).setValue(
                                Float.parseFloat(target.getLabel().substring(0, target.getLabel().length() - 1)));
                    } else if (target.getLabel().endsWith("L")) {
                        ((CtLiteral<Long>) parent).setValue(
                                Long.parseLong(target.getLabel().substring(0, target.getLabel().length() - 1)));
                    } else if (target.getLabel().endsWith("D")) {
                        ((CtLiteral<Double>) parent).setValue(
                                Double.parseDouble(target.getLabel().substring(0, target.getLabel().length())));
                    } else {
                        try {
                            ((CtLiteral<Object>) parent).setValue(Integer.parseInt(target.getLabel()));
                        } catch (Exception e) {
                            ((CtLiteral<Double>) parent).setValue(Double.parseDouble(target.getLabel()));
                        }
                    }
                } else if (parent instanceof CtFieldAccess) {
                    CtVariableReference ref = factory.createFieldReference();
                    ref.setSimpleName(target.getLabel());
                    ((CtFieldAccess<?>) parent).setVariable(ref);
                    ((CtFieldAccess<?>) parent).getTarget();
                } else if (parent instanceof CtVariableAccess) {
                    CtVariableReference ref = factory.createLocalVariableReference();
                    ref.setSimpleName(target.getLabel());
                    ((CtVariableAccess<?>) parent).setVariable(ref);
                } else if (parent instanceof CtInvocation) {
                    CtExecutableReference ref = factory.createExecutableReference();
                    ref.setSimpleName(target.getLabel());
                    ((CtInvocation<?>) parent).setExecutable(ref);
                    ref.getDeclaration();
                    ((CtInvocation<?>) parent).getTarget();
                } else if (parent instanceof CtTypeAccess) {
                    CtTypeAccess sp = (CtTypeAccess) source.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                    CtTypeReference ref = factory.Type().createReference(sp.getAccessedType().getQualifiedName());
                    if (ref.getPackage() != null)
                        ref.getPackage().setImplicit(true);
                    if (ref.getDeclaringType() != null)
                        ref.getDeclaringType().setImplicit(true);
                    ref.setSimpleName(target.getLabel());
                    ref.setImplicit(((CtTypeAccess<?>) parent).isImplicit());
                    ((CtTypeAccess<?>) parent).setAccessedType(ref);
                    CtTargetedExpression parentparent = (CtTargetedExpression) parent.getParent();
                    parentparent.setTarget((CtTypeAccess<?>) parent);
                } else if (parent instanceof CtThisAccess) { // TODO shouldn't get up to there
                    // CtThisAccess ref = factory.createThisAccess();
                    // ref.setSimpleName(target.getLabel());
                    // ref.setImplicit(((CtTypeAccess<?>) parent).isImplicit());
                    // ((CtThisAccess<?>) parent).setAccessedType(ref);
                } else if (parent instanceof CtOperatorAssignment) {
                    ((CtOperatorAssignment) parent).setKind(MyUtils.getBinaryOperatorByName(target.getLabel()));
                } else if (parent instanceof CtAssignment) { // TODO shouldn't get up to there
                } else if (parent instanceof CtReturn) { // TODO shouldn't get up to there
                    // CtFieldWrite w = factory.createFieldWrite();
                    // CtFieldReference v = factory.createFieldReference();
                    // v.setSimpleName(target.getLabel());
                    // w.setVariable(v);
                    // ((CtAssignment) parent).setAssigned(w);

                    // ((CtAssignment) parent).setLabel(target.getLabel());
                } else {
                    System.err.println(parent.getClass());
                }
                break;
            }
            case "Interface": {
                System.out.println("isInterface");
                CtInterface<?> interf = factory.createInterface();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                interf.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, interf);
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent == null) {
                    factory.getModel().getRootPackage().addType(interf);
                } else if (parent instanceof CtPackage) {
                    interf.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
                    ((CtPackage) parent).addType(interf);
                } else {
                    ((CtType) parent).addNestedType(interf);
                }
                break;
            }
            case "Class": {
                System.out.println("isClass");
                CtClass<?> clazz = factory.createClass();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                clazz.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, clazz);
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent == null) {
                    factory.getModel().getRootPackage().addType(clazz);
                } else if (parent instanceof CtPackage) {
                    clazz.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
                    ((CtPackage) parent).addType(clazz);
                } else {
                    clazz.setSimpleName("PlaceHolder" + ((CtType) parent).getNestedTypes().size());
                    ((CtType) parent).addNestedType(clazz);
                }
                break;
            }
            case "RootPackage": {
                System.out.println("isRootPackage");
                CtPackage pack = factory.getModel().getRootPackage();
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, pack);
                break;
            }
            case "Package": {
                System.out.println("isPackage");
                CtPackage pack;
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent == null || parent instanceof CtRootPackage) {
                    String name = "placeholderpack" + factory.getModel().getRootPackage().getPackages().size();
                    pack = factory.Package().getOrCreate(name);
                } else {
                    String name = "placeholderpack" + ((CtPackage) parent).getPackages().size();
                    pack = factory.createPackage((CtPackage) parent, name);
                }
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, pack);
                break;
            }
            case "Method": {
                System.out.println("isMethod");
                CtMethod<Object> method = factory.createMethod();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                method.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                method.setDefaultMethod(((CtMethod)sp).isDefaultMethod());
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, method);
                CtType<?> parent = (CtType<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                parent.addMethod(method);
                method.setSimpleName("placeHolder" + parent.getMethods().size());
                if (method.isAbstract()) {
                    break;
                }
                if (parent instanceof CtInterface && !method.isDefaultMethod()) {
                    break;
                }
                method.setBody(factory.createBlock());
                break;
            }
            case "RETURN_TYPE": {
                System.out.println("isReturnType");
                CtTypeReference ref = factory.createTypeReference();
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                ((CtTypedElement<?>) parent).setType(ref);
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
                break;
            }
            case "MODIFIER": {
                System.out.println("isMOdifier");
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                CtWrapper<CtExtendedModifier> mod = (CtWrapper<CtExtendedModifier>) source
                        .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (!mod.getValue().isImplicit())
                    ((CtModifiable) parent).addModifier(mod.getValue().getKind());
                if (parent instanceof CtMethod && ((CtMethod) parent).isStatic()
                        && ((CtMethod) parent).getBody() == null)
                    ((CtMethod) parent).setBody(factory.createBlock());
                break;
            }
            case "Field": {
                System.out.println("isField");
                CtField<?> field = factory.createField();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                field.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, field);
                CtType<?> parent = (CtType<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                field.setSimpleName("placeHolder" + parent.getFields().size());
                parent.addField(field);
                break;
            }
            case "VARIABLE_TYPE": {
                System.out.println("isVarType");
                CtTypeReference ref = factory.createTypeReference();
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                ((CtTypedElement<?>) parent).setType(ref);
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
                break;
            }
            case "Literal": {
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                CtExpression created = factory.createLiteral();
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                if (parent == null) {
                } else if (parent instanceof CtRHSReceiver) {
                    ((CtRHSReceiver<?>) parent).setAssignment(created);
                } else if (parent instanceof CtReturn) {
                    ((CtReturn<?>) parent).setReturnedExpression(created);
                } else if (parent instanceof CtUnaryOperator) {
                    ((CtUnaryOperator<?>) parent).setOperand(created);
                } else {
                    CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                    if (sp.getRoleInParent().equals(CtRole.LEFT_OPERAND))
                        ((CtBinaryOperator<?>) parent).setLeftHandOperand(created);
                    else if (sp.getRoleInParent().equals(CtRole.RIGHT_OPERAND))
                        ((CtBinaryOperator<?>) parent).setRightHandOperand(created);
                    else
                        throw new UnsupportedOperationException(sp.getRoleInParent().name() + " role not supported");
                }
                break;
            }
            case "BinaryOperator": {
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                CtExpression created = factory.createBinaryOperator();
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                if (parent instanceof CtField) {
                    ((CtField<?>) parent).setDefaultExpression(created);
                } else if (parent instanceof CtReturn) {
                    ((CtReturn<?>) parent).setReturnedExpression(created);
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                break;
            }
            case "UnaryOperator": {
                CtExpression created = factory.createUnaryOperator();
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent == null) {
                } else if (parent instanceof CtField) {
                    ((CtField<?>) parent).setDefaultExpression(created);
                } else if (parent instanceof CtReturn) {
                    ((CtReturn<?>) parent).setReturnedExpression(created);
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                break;
            }
            case "FieldRead": {
                System.out.println("isFieldRead");
                CtFieldRead created = factory.createFieldRead();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                if (parent instanceof CtInvocation) {
                    ((CtInvocation<?>) parent).setTarget(created);
                } else if (parent instanceof CtUnaryOperator) {
                    ((CtUnaryOperator<?>) parent).setOperand(created);
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                break;
            }
            case "VariableRead": {
                System.out.println("isVariableRead");
                CtVariableRead created = factory.createVariableRead();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent instanceof CtInvocation) {
                    ((CtInvocation<?>) parent).addArgument(created);
                } else if (parent instanceof CtBinaryOperator) {
                    if (sp.getRoleInParent().equals(CtRole.LEFT_OPERAND)) {
                        ((CtBinaryOperator<?>) parent).setLeftHandOperand(created);
                    } else if (sp.getRoleInParent().equals(CtRole.RIGHT_OPERAND)) {
                        ((CtBinaryOperator<?>) parent).setRightHandOperand(created);
                    } else {
                        throw new UnsupportedOperationException(sp.getRoleInParent().name() + " role not supported");
                    }
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                break;
            }
            case "FieldWrite": {
                System.out.println("isFieldWrite");
                CtFieldWrite created = factory.createFieldWrite();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent instanceof CtUnaryOperator) {
                    ((CtUnaryOperator<?>) parent).setOperand(created);
                } else if (parent instanceof CtAssignment) {
                    ((CtAssignment<?, ?>) parent).setAssigned(created);
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                // factory.createTypeAcc
                // CtTypeReference auxRef = factory.createTypeReference();
                // auxRef.setSimpleName(parent.); // TODO need an helper to seach first parent
                // that contains this type
                // // with an implicit target I shound not need to specify it
                // CtTypeAccess defaultRef = factory.createTypeAccess(auxRef, true);
                // created.setTarget(defaultRef);
                break;
            }
            case "TypeAccess": {
                System.out.println("isTypeAccess");
                CtTypeAccess created = factory.createTypeAccess();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                created.setImplicit(((CtTypeAccess) sp).isImplicit());
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                CtTargetedExpression<?, ?> parent = (CtTargetedExpression<?, ?>) parentTarget
                        .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent instanceof CtFieldAccess) {
                    ((CtFieldAccess<?>) parent).setTarget(created);
                } else if (parent instanceof CtThisAccess) {
                    ((CtThisAccess<?>) parent).setTarget(created);
                } else if (parent instanceof CtInvocation) {
                    ((CtInvocation<?>) parent).setTarget(created);
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                break;
            }
            case "AnonymousExecutable": {
                System.out.println("isAnonymousExecutable");
                CtAnonymousExecutable created = factory.createAnonymousExecutable();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                CtClass<?> parent = (CtClass<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                parent.addAnonymousExecutable(created);
                break;
            }
            case "Assignment": {
                System.out.println("isAssignment");
                CtAssignment created = factory.createAssignment();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                CtAnonymousExecutable parent = (CtAnonymousExecutable) parentTarget
                        .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                addInBody(factory, target, created, parent);
                // if (parent.getBody() == null) {
                //     parent.setBody(factory.createBlock());
                // }
                // int i = 0;
                // for (AbstractVersionedTree aaa : parentTarget.getAllChildren()) {
                //     if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) == null) {
                //         i++;
                //     }
                // }
                // parent.getBody().addStatement(i, created);
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                break;
            }
            case "OperatorAssignment": {
                System.out.println("isOperatorAssignment");
                CtOperatorAssignment created = factory.createOperatorAssignment();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                created.setKind(BinaryOperatorKind.MINUS);
                CtAnonymousExecutable parent = (CtAnonymousExecutable) parentTarget
                        .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                addInBody(factory, target, created, parent);
                // if (parent.getBody() == null) {
                //     parent.setBody(factory.createBlock());
                // }
                // int i = 0;
                // for (AbstractVersionedTree aaa : parentTarget.getAllChildren()) {
                //     if (aaa.getMetadata("type").equals("MODIFIER")) {
                //         continue;
                //     }
                //     if (aaa.getMetadata("type").equals("RETURN_TYPE")) {
                //         continue;
                //     }
                //     if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null) {
                //         i++;
                //     }
                // }
                // parent.getBody().addStatement(i, created);
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                break;
            }
            case "Return": {
                System.out.println("isReturn");
                CtReturn created = factory.createReturn();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                CtMethod parent = (CtMethod) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                addInBody(factory, target, (CtStatement) created, parent);
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                break;
            }
            case "Invocation": {
                System.out.println("isInvocation");
                CtInvocation created = factory.createInvocation();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                // CtReturn<?> parent = (CtReturn<?>)
                // parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setTarget(factory.createThisAccess(
                        (parent instanceof CtType ? (CtType) parent : parent.getParent(CtType.class)).getReference(),
                        true));
                if (parent instanceof CtReturn) {
                    ((CtReturn<?>) parent).setReturnedExpression(created);
                } else if (parent instanceof CtExecutable) {
                    addInBody(factory, target, (CtStatement) created, (CtExecutable<?>) parent);
                } else if (parent instanceof CtRHSReceiver) {
                    ((CtRHSReceiver<?>) parent).setAssignment(created);
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                break;
            }
            case "ThisAccess": {
                System.out.println("isThisAccess");
                CtThisAccess created = factory.createThisAccess();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                created.setImplicit(((CtThisAccess) sp).isImplicit());
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
                CtTargetedExpression<?, ?> parent = (CtTargetedExpression<?, ?>) parentTarget
                        .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (parent instanceof CtFieldAccess) {
                    ((CtFieldAccess<?>) parent).setTarget(created);
                } else if (parent instanceof CtInvocation) {
                    ((CtInvocation<?>) parent).setTarget(created);
                } else {
                    throw new UnsupportedOperationException(
                            parent.getClass().toString() + " as a parent is no handled");
                }
                break;
            }
            case "Parameter": {
                System.out.println("isParameter");
                CtParameter<?> field = factory.createParameter();
                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                field.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, field);
                CtMethod<?> parent = (CtMethod<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                parent.addParameter(field);
                break;
            }
            case "TypeReference": {
                System.out.println("isTypeReference");
                CtTypeReference ref = factory.createTypeReference();
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                // ((CtType<?>) parent).setSuperclass(ref);
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
                break;
            }
            case "SUPER_CLASS": {
                System.out.println("isTypeReference");
                CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                CtType<?> parentType = (CtType<?>) parentTarget.getParent()
                        .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                parentType.setSuperclass((CtTypeReference) parent);
                target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, (CtTypeReference) parent);
                break;
            }
            default: {
                System.err.println(targetType);
                throw new AssertionError(targetType + " is no handled");
                // throw new UnsupportedOperationException(targetType + " is no handled");
            }
        }
    }

    private static void addInBody(Factory factory, AbstractVersionedTree target, CtStatement created,
            CtExecutable<?> parent) {
        if (parent.getBody() == null) {
            parent.setBody(factory.createBlock());
        }
        int i = 0;
        for (AbstractVersionedTree aaa : target.getParent().getAllChildren()) {
            if (aaa.getMetadata("type").equals("MODIFIER")) {
                continue;
            }
            if (aaa.getMetadata("type").equals("RETURN_TYPE")) {
                continue;
            }
            if (aaa.getMetadata("type").equals("LABEL")) {
                continue;
            }
            if (aaa.getMetadata("type").equals("Parameter")) {
                continue;
            }
            if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null) {
                i++;
            }
            // if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) == null || aaa.getMetadata("type").equals("LABEL")
            //         || aaa.getMetadata("type").equals("MODIFIER") || aaa.getMetadata("type").equals("RETURN_TYPE")) {
            //     continue;
            // }
            // i++;
        }
        parent.getBody().addStatement(i, created);
    }

    //     static String[] LARGE = new String[]{
    // "
    // package a.bcx9.ge;
    // class Uts extends a.bcx9.LHO1_1_ {}

    // package a;
    // class U67 {
    //     class R_J__6Zz__ extends a.U67 {
    //         private protected public abstract final synchronized transient java.lang.Integer q_slk() {
    //             return null;
    //         }

    //         private protected public abstract final native synchronized transient a.U67.R_J__6Zz__ i__978___;

    //         class Rkg_i {
    //             private protected abstract static strictfp synchronized transient java.lang.Integer x_XT_T7O;
    //         }
    //     }
    // }

    // package g2_9;
    // class LC4_V_h4 {}

    // package a.bcx9;
    // class LHO1_1_ {
    //     class T71c_a4r extends a.U67 {}

    //     public abstract final transient volatile a.bcx9.LHO1_1_.T71c_a4r jsDI6Q___() {
    //         return null;
    //     }
    // }

    // package a.bcx9;
    // class P7J2609x_5 extends a.bcx9.P7J2609x_5.R_J__6Zz__ {
    //     interface V9_H_Z4w0W_ {
    //         abstract static synchronized transient volatile a.bcx9.LHO1_1_ i_o89ZwGb2_;
    //     }

    //     protected abstract strictfp synchronized transient volatile a.bcx9.LHO1_1_ xEf() {
    //         return null;
    //     }

    //     class RwC_G__yB {
    //         class DG_ extends g2_9.LC4_V_h4 {
    //             protected public abstract final native strictfp volatile java.util.Set u0_8_() {
    //                 return null;
    //             }
    //         }

    //         interface P1Kj4 {
    //             abstract final native strictfp synchronized transient a.bcx9.LHO1_1_ pu1;

    //             final native synchronized volatile java.util.Date a_0U();
    //         }
    //     }
    // }
    // "
    //     };
}