package gumtree.spoon;

import org.junit.Before;
import org.junit.Test;

public class SimpleDeleteApplyTest {
    @Before
    public void initProps() {
        System.setProperty("nolabel", "true");
    }
    
    @Test
    public void testSimpleApplyDeleteInterface() {
        String contents = "interface X {}";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceMeth() {
        String contents = "interface X { static void f(){} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceMethDecl() {
        String contents = "interface X { void f(); }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldInteger() {
        String contents = "interface X { int value = 0; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteHexInt() {
        String contents = "interface X { int value = 0xffff00f; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteHexLong() {
        String contents = "interface X { long value = 0xffff00ffL; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceUnicode() {
        String contents = "interface X { char value = '\\uf127'; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteNewLineChar() {
        String contents = "interface X { char value = '\n'; }";
        ApplyTestHelper.onDelete(contents);
    }
    @Test
    public void testSimpleApplyDeleteNewLineChar2() {
        String contents = "interface X { char value = '\\n'; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceUnicode2() {
        String contents = "interface X { char value = '\uf127'; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldStringNum() {
        String contents = "interface X { String value = \"54246\"; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldString2() {
        String contents = "interface X { String value = \"\\u0087\"; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldString() {
        String contents = "interface X { String value = \"\"; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteStringQuote() {
        String contents = "interface X { String value = \"Can't skid into hex\"; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldBooleanTrue() {
        String contents = "interface X { boolean value = true; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldBooleanFalse() {
        String contents = "interface X { boolean value = false; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldNull() {
        String contents = "interface X { String value = null; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldChar() {
        String contents = "interface X { char value = 'c'; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldFloat() {
        String contents = "interface X { float value = 0.1F; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldLong() {
        String contents = "interface X { long value = 0l; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldDouble() {
        String contents = "interface X { double value = 0.0d; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceField1PLUS1() {
        String contents = "interface X { int value = 1+1; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceField1PLUS2() {
        String contents = "interface X { int value = 1+2; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceFieldInc() {
        String contents = "interface X { int value = 1++; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteFieldsNeg() {
        String contents = "class X { int value = 1; int value2 = -value; }";
        ApplyTestHelper.onDelete(contents);
    }
    @Test
    public void testSimpleApplyDeleteInterfaceFieldsNegFull() {
        String contents = "interface X { static int value = 1; int value2 = -X.value; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterface2() {
        String contents = "package org; interface X {}";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteInterfaceMethEmptRet() {
        String contents = "interface X { static void f(){ return;} }";
        ApplyTestHelper.onDelete(contents);
    }
    @Test
    public void testSimpleApplyDeleteRec() {
        String contents = "interface X { static void f(){ f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteCallUknown() {
        String contents = "public class X { static void f(){ Y.f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteCallUknown2() {
        String contents = "public class X { static void f(){ int i = Y.f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteCallUknown3() {
        String contents = "public class X { static void f(){ int i = 0; i = Y.f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteCallUknown4() {
        String contents = "package a.b; public class X { static void f(){ boolean b = true; b|=Y.f(0,Y.FIELD).get()!=3;} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteCallUknown5() {
        String contents = "package a.b; public class X { void f(){ Y.g(Y.FIELD).get();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteCallUknown6() {
        String contents = "package a.b; public class X { static void f(){ Y.i.toString(Y.y).toString();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClass() {
        String contents = "class X {}";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassFieldsInc() {
        String contents = "class X { static int value = 1; static int value2 = value++; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassFieldsStaticConstr() {
        String contents = "class X { static int value; static {value = 1;} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteFor() {
        String contents = "class X { static {for(;;){}} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteFor2() {
        String contents = "class X { static {for(;;);} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteFor3() {
        String contents = "class X {static {int j=0; for(int i;;j--){}} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteFor4() {
        String contents = "class X {static {int i=4; for(;i<3;){i=2;}} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteIf1() {
        String contents = "class X {static {int i=4; if(i<3)i=7;} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteAssignCall() {
        String contents = "class X {static int f(){return 1;} static {int i=X.f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteAssignCall2() {
        String contents = "class X {static int i; static int f(){return 1;} static {i=X.f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassFieldsStaticConstrInc() {
        String contents = "public class X { static int value = 0; static {value += 1;} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteNewArray() {
        String contents = "public class X { static int[] value = new int[10];}";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteNewArray2() {
        String contents = "class X { void f() {int[] value = new int[10];} }";
        ApplyTestHelper.onDelete(contents);
    }


    @Test
    public void testSimpleApplyDeleteClassMethodRet1() {
        String contents = "class X { int f() {return 1;} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassMethodRet1Plus1() {
        String contents = "class X { int f() {return 1 + 1;} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassMethodRec() {
        String contents = "class X { int f() {return f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassMethodRec2() {
        String contents = "class X { int f() {return X.this.f();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteMeth() {
        String contents = "class y {} class X { void f() {y.g();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteMeth2() {
        String contents = "import Y; class X { void f() {Y.g();} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassMethodParam() {
        String contents = "class X { void f(int i) {} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassMethodParam2() {
        String contents = "class X { void f(int i, int j) {} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassMethodParam2X() {
        String contents = "class X { void f(X i, X j) {} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassMethodField() {
        String contents = "class X { int i = 0; void f() { i = 3;} }";
        ApplyTestHelper.onDelete(contents);
    }
    
    @Test
    public void testSimpleApplyDeleteClassDoubleMethod() {
        String contents = "class X { int add(int i) { return i + i; } }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassAdditionMethod() {
        String contents = "class X { int add(int i, int j) { return i + j; } }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassPrintMethod() {
        String contents = "class X { void print(String s) { System.out.println(s); } }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassInterface() {
        String contents = "class X { interface Y {} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassClass() {
        String contents = "class X { class Y extends X {} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassClassExt() {
        String contents = "class X extends X.Y { class Y {} }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassConstr() {
        String contents = "class X { X(){}; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassInst() {
        String contents = "class X { X inst = new X(); }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassCond() {
        String contents = "class X { int i = 1 > 2 ? 3 : 4 ; }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassImplemInterface() {
        String contents = "class X implements Runnable { }";
        ApplyTestHelper.onDelete(contents);
    }

    @Test
    public void testSimpleApplyDeleteClassInterface5() {
        String contents = "interface A {public abstract synchronized strictfp transient final void f();}";
        ApplyTestHelper.onDelete(contents);
    }
    
    @Test
    public void testSimpleApplyDeleteSuper1() {
        String contents = "class X extends Y { X() { super(); } }";
        ApplyTestHelper.onDelete(contents);
    }
    
    @Test
    public void testSimpleApplyDeleteThis() {
        String contents = "class X extends Y { X() { this(0); } X(int i) {} }";
        ApplyTestHelper.onDelete(contents);
    }
    
}