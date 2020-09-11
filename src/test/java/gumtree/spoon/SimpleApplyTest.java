package gumtree.spoon;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import spoon.MavenLauncher;
import spoon.SpoonModelBuilder;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtRHSReceiver;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.reflect.CtExtendedModifier;

public class SimpleApplyTest {
    @Before
    public void initProps() {
        System.setProperty("nolabel", "true");
    }

    private void insertTestHelper(String contents) {
        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        Factory left = Utils.makeFactory();
        Factory right = Utils.makeFactory(new VirtualFile(contents, "X.java"));

        ITree srctree = scanner.getTree(left.getModel().getRootPackage());
        MultiDiffImpl mdiff = new MultiDiffImpl(srctree);

        ITree dstTree = scanner.getTree(right.getModel().getRootPackage());
        DiffImpl diff = mdiff.compute(scanner.getTreeContext(), dstTree);

        List<Operation> ops = diff.getAllOperations();
        for (Operation<?> op : ops) {
            Action action = op.getAction();
            System.out.println(action.format(scanner.getTreeContext()));
            if (action instanceof AAction) {
                AAction aaction = (AAction) action;
                ITree leftNode = aaction.getSource();
                System.out.println("\t" + Utils.toPrettyString(scanner.getTreeContext(), leftNode));
                ITree rightNode = aaction.getTarget();
                System.out.println("\t" + Utils.toPrettyString(scanner.getTreeContext(), rightNode));
                if (aaction instanceof Update) {
                    System.out.println("\t" + leftNode.getMetadata("type") + "\t"
                            + ((AbstractVersionedTree) leftNode).getAllChildren().size());
                    System.out.println("\t" + rightNode.getMetadata("type") + "\t"
                            + ((AbstractVersionedTree) rightNode).getAllChildren().size());
                }
            }
        }

        System.out.println(".......0........");
        ITree middle = mdiff.getMiddle();
        System.out.println(Utils.toPrettyTree(scanner.getTreeContext(), dstTree));
        System.out.println(Utils.toPrettyTree(scanner.getTreeContext(), middle));
        System.out.println(".......1........");

        CtRootPackage middleRP = (CtRootPackage) middle.getChild(0).getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        System.out.println(middleRP);
        System.out.println(diff.getActionsList());
        System.out.println("........2.......");
        for (Action action : diff.getActionsList()) {
            applyAInsert(middleRP.getFactory(), scanner.getTreeContext(), (Insert & AAction<Insert>) action);
        }
        System.out.println(".......3........");

        PrettyPrinter pp = middleRP.getFactory().getEnvironment().createPrettyPrinter();
        for (CtType<?> type : middleRP.getTypes()) {
            type.getPosition().getCompilationUnit();
            System.out.println("......." + type.getPosition().getFile().getAbsolutePath() + "........");
            System.out.println(pp.prettyprint(type));
        }
        System.out.println("======");
        for (CtType<?> type : right.getModel().getAllTypes()) {
            System.out.println("......." + type.getPosition().getFile().getAbsolutePath() + "........");
            System.out.println(pp.prettyprint(type));
        }
        assertEquals(pp.prettyprint(right.getModel().getAllTypes().iterator().next()), pp.prettyprint(middleRP.getTypes().iterator().next()));
        System.out.println(".......4........");
    }

    @Test
    public void testSimpleApplyInsertInterface() {
        String contents = "interface X {}";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceMeth() {
        String contents = "interface X { void f(){} }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldInteger() {
        String contents = "interface X { int value = 0; }";
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
    public void testSimpleApplyInsertInterfaceFieldAdd() {
        String contents = "interface X { int value = 1+1; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldInc() {
        String contents = "interface X { int value = 1++; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceField5() {
        String contents = "interface X { int value = 1; int value2 = value++; }";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertInterface2() {
        String contents = "package org; interface X {}";
        insertTestHelper(contents);
    }

    @Test
    public void testSimpleApplyInsertClass() {
        String contents = "package org; class X {}";
        insertTestHelper(contents);
    }

    private void applyAInsert(Factory factory, TreeContext ctx, AAction<Insert> action) {
        ITree source = action.getSource();
        AbstractVersionedTree target = action.getTarget();
        AbstractVersionedTree parentTarget = target.getParent();
        System.out.println("=======");
        System.out.println(Utils.toPrettyString(ctx, source));
        System.out.println(Utils.toPrettyString(ctx, target));
        System.out.println(Utils.toPrettyString(ctx, parentTarget));
        if (target.getMetadata("type").equals("LABEL")) {
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
                ((CtBinaryOperator<?>) parent).setKind(Utils.getBinaryOperatorByName(target.getLabel()));
            } else if (parent instanceof CtUnaryOperator) {
                ((CtUnaryOperator<?>) parent).setKind(Utils.getUnaryOperatorByName(target.getLabel()));
            } else if (parent instanceof CtLiteral) {
                if (target.getLabel().startsWith("\"")) {
                    ((CtLiteral<Object>) parent)
                            .setValue(target.getLabel().substring(1, target.getLabel().length() - 1));
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
                    ((CtLiteral<Float>) parent)
                            .setValue(Float.parseFloat(target.getLabel().substring(0, target.getLabel().length() - 1)));
                } else if (target.getLabel().endsWith("L")) {
                    ((CtLiteral<Long>) parent)
                            .setValue(Long.parseLong(target.getLabel().substring(0, target.getLabel().length() - 1)));
                } else if (target.getLabel().endsWith("D")) {
                    ((CtLiteral<Double>) parent)
                            .setValue(Double.parseDouble(target.getLabel().substring(0, target.getLabel().length())));
                } else {
                    try {
                        ((CtLiteral<Object>) parent).setValue(Integer.parseInt(target.getLabel()));
                    } catch (Exception e) {
                        ((CtLiteral<Double>) parent).setValue(Double.parseDouble(target.getLabel()));
                    }
                }
            } else {
                System.err.println(parent.getClass());
            }
        } else if (target.getMetadata("type").equals("Interface")) {
            System.out.println("isInterface");
            CtInterface<?> interf = factory.createInterface("PlaceHolder");
            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            interf.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, interf);
        } else if (target.getMetadata("type").equals("Class")) {
            System.out.println("isClass");
            CtClass<?> clazz = factory.createClass("PlaceHolder");
            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            clazz.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, clazz);
        } else if (target.getMetadata("type").equals("Package")) {
            System.out.println("isPackage");
            CtPackage pack = factory.createPackage();
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, pack);
        } else if (target.getMetadata("type").equals("Method")) {
            System.out.println("isMethod");
            CtMethod<Object> method = factory.createMethod();
            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            method.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, method);
            CtType<?> parent = (CtType<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            parent.addMethod(method);
        } else if (target.getMetadata("type").equals("RETURN_TYPE")) {
            System.out.println("isReturnType");
            CtTypeReference ref = factory.createTypeReference();
            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            ((CtTypedElement<?>) parent).setType(ref);
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
        } else if (target.getMetadata("type").equals("MODIFIER")) {
            System.out.println("isMOdifier");
            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            CtWrapper<CtExtendedModifier> mod = (CtWrapper<CtExtendedModifier>) source
                    .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            if (!mod.getValue().isImplicit())
                ((CtModifiable) parent).addModifier(mod.getValue().getKind());
        } else if (target.getMetadata("type").equals("Field")) {
            System.out.println("isField");
            CtField<?> field = factory.createField();
            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            field.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, field);
            CtType<?> parent = (CtType<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            parent.addField(field);
        } else if (target.getMetadata("type").equals("VARIABLE_TYPE")) {
            System.out.println("isVarType");
            CtTypeReference ref = factory.createTypeReference();
            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            ((CtTypedElement<?>) parent).setType(ref);
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
        } else if (target.getMetadata("type").equals("Literal")) {
            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            CtExpression lit = factory.createLiteral();
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, lit);
            if (parent instanceof CtRHSReceiver) {
                ((CtRHSReceiver<?>) parent).setAssignment(lit);
            } else {
                ((CtBinaryOperator<?>) parent).setLeftHandOperand(lit);
                ((CtBinaryOperator<?>) parent).setRightHandOperand(lit);
            }
        } else if (target.getMetadata("type").equals("BinaryOperator")) {
            CtField<?> parent = (CtField<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            CtExpression bin = factory.createBinaryOperator();
            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, bin);
            parent.setDefaultExpression(bin);
        } else {
            System.err.println(target.getMetadata("type"));
        }
    }
}