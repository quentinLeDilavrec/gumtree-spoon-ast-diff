package gumtree.spoon;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.lang.model.util.ElementScanner6;

import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractTree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.VersionedTree;
import com.github.gumtreediff.tree.AbstractTree.FakeTree;

import org.junit.Before;
import org.junit.Test;

import gumtree.spoon.apply.Combination;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.apply.operations.MyCloneHelper;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import gumtree.spoon.diff.support.SpoonSupport;
import spoon.ContractVerifier;
import spoon.MavenLauncher;
import spoon.SpoonModelBuilder;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.compiler.Environment.PRETTY_PRINTING_MODE;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.LiteralBase;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.CtScanner;
import spoon.support.DefaultCoreFactory;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

public class SimpleApplyTest {
    @Before
    public void initProps() {
        System.setProperty("nolabel", "true");
    }

    @Test
    public void testSimpleApplyInsertInterface() {
        String contents = "interface X {}";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceMeth() {
        String contents = "interface X { static void f(){} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceMethDecl() {
        String contents = "interface X { void f(); }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldInteger() {
        String contents = "interface X { int value = 0; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceUnicode() {
        String contents = "interface X { char value = '\\uf127'; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceUnicode2() {
        String contents = "interface X { char value = '\uf127'; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldStringNum() {
        String contents = "interface X { String value = \"54246\"; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldString2() {
        String contents = "interface X { String value = \"\\u0087\"; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldString() {
        String contents = "interface X { String value = \"\"; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldBooleanTrue() {
        String contents = "interface X { boolean value = true; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldBooleanFalse() {
        String contents = "interface X { boolean value = false; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldNull() {
        String contents = "interface X { String value = null; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldChar() {
        String contents = "interface X { char value = 'c'; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldFloat() {
        String contents = "interface X { float value = 0.1f; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldLong() {
        String contents = "interface X { long value = 0l; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldDouble() {
        String contents = "interface X { double value = 0.0d; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceField1PLUS1() {
        String contents = "interface X { int value = 1+1; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceField1PLUS2() {
        String contents = "interface X { int value = 1+2; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldInc() {
        String contents = "interface X { int value = 1++; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldsNeg() {
        String contents = "interface X { int value = 1; int value2 = -value; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceFieldsNegFull() {
        String contents = "interface X { int value = 1; int value2 = -X.value; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterface2() {
        String contents = "package org; interface X {}";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertInterfaceMethEmptRet() {
        String contents = "interface X { static void f(){ return;} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClass() {
        String contents = "class X {}";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassFieldsInc() {
        String contents = "class X { static int value = 1; static int value2 = value++; }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassFieldsStaticConstr() {
        String contents = "class X { static int value; static {value = 1;} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassFieldsStaticConstrInc() {
        String contents = "public class X { static int value = 0; static {value += 1;} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRet1() {
        String contents = "class X { int f() {return 1;} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRet1Plus1() {
        String contents = "class X { int f() {return 1 + 1;} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRec() {
        String contents = "class X { int f() {return f();} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodRec2() {
        String contents = "class X { int f() {return X.this.f();} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodParam() {
        String contents = "class X { void f(int i) {} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodParam2() {
        String contents = "class X { void f(int i, int j) {} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassMethodParam2X() {
        String contents = "class X { void f(X i, X j) {} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassDoubleMethod() {
        String contents = "class X { int add(int i) { return i + i; } }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassAdditionMethod() {
        String contents = "class X { int add(int i, int j) { return i + j; } }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassPrintMethod() {
        String contents = "class X { void print(String s) { System.out.println(s); } }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassInterface() {
        String contents = "class X { interface Y {} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassClass() {
        String contents = "class X { class Y extends X {} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassClassExt() {
        String contents = "class X extends X.Y { class Y {} }";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertClassClassExt2() {
        String contents = "package x.a; class X { class Y extends x.b.X.Y {} }";
        String contents1 = "package x.b; class X { class Y extends x.a.X {} }";
        Factory right = MyUtils.makeFactory(new VirtualFile(contents, "x/a/X.java"),
                new VirtualFile(contents1, "x/b/X.java"));
        ApplyTestHelper.onInsert(right.getModel().getRootPackage());
    }

    @Test
    public void testSimpleApplyInsertClassClassExt3() {
        String contents = "package x.a; class X { class Y {} }";
        String contents1 = "package x.b; class X extends X.Y { class Y {} }";
        Factory right = MyUtils.makeFactory(new VirtualFile(contents, "x/a/X.java"),
                new VirtualFile(contents1, "x/b/X.java"));
        ApplyTestHelper.onInsert(right.getModel().getRootPackage());
    }

    @Test
    public void testSimpleApplyInsertClassInterface5() {
        String contents = "interface A {public abstract synchronized strictfp transient final void f();}";
        ApplyTestHelper.onInsert(contents);
    }

}