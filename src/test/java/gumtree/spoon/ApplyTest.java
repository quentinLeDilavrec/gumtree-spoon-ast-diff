package gumtree.spoon;

import static org.junit.Assert.assertTrue;

import java.io.File;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionInt;

import org.junit.Before;
import org.junit.Test;

import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import spoon.MavenLauncher;
import spoon.compiler.Environment;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.factory.Factory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;

public class ApplyTest {
        @Before
        public void initProps() {
                System.setProperty("nolabel", "true");
        }

        @Test
        public void testVersion() {
                Version v0 = new VersionInt(0);
                Version v1 = new VersionInt(1);
                Version v2 = new VersionInt(2);
                assertTrue(v0.compareTo(v0)==0);
                assertTrue(v0.compareTo(v1)<0);
                assertTrue(v0.compareTo(v2)<0);
                assertTrue(v1.compareTo(v0)>0);
        }

        @Test
        public void testSimpleApply() {
                String contentsLeft = "class X { " + "}";
                String contentsRight = "class Y { " + "}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }

        @Test
        public void testSimpleApplyA() {
                String contentsLeft = "interface X { public void f() {}}";
                String contentsRight = "interface X { void f() {}}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }

        @Test
        public void testSimpleApplyAA() {
                String contentsLeft = "class X {void f() {}}";
                String contentsRight = "class X {public void f() {}}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }

        @Test
        public void testSimpleApplyB() {
                String contentsLeft = "public class X { String f(){return \"Hello World!\";}}";
                String contentsRight = "public class X { String f(){return \"Hello!\";}}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }

        @Test
        public void testSimpleApplyC() {
            String contentsLeft = "public class X { static void f(){} static void g(){}}";
            String contentsLeftT = "public class XTest { @Test void fTest(){X.f();} @Test void gTest(){X.g();}}";
            String contentsRight = "public class X { static void f(){} public static class Y {static void g(){} static void h(){}}}";
            String contentsRightT = "public class XTest { @org.junit.Test void fTest(){X.f();} @Test void gTest(){X.Y.g();} @Test void hTest(){X.Y.h();}}";
            Factory left = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java")
        //     ,new VirtualFile(contentsLeftT, "XTest.java")
                    );
            for (CompilationUnit cu : left.CompilationUnit().getMap().values()) {
                if (cu.getFile().getName().contains("Test")) {
                    cu.putMetadata("isTest", true);
                }
            }
            Factory right = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java")
        //     ,                    new VirtualFile(contentsRightT, "XTest.java")
            );
            for (CompilationUnit cu : right.CompilationUnit().getMap().values()) {
                if (cu.getFile().getName().contains("Test")) {
                    cu.putMetadata("isTest", true);
                }
            }
            ApplyTestHelper.onChange(left.getModel().getRootPackage(), right.getModel().getRootPackage());
        }
        @Test
        public void testSimpleApplySuper2() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/roots/test9/left_QuickNotepad_1.13.java"),
                                new File("src/test/resources/examples/roots/test9/right_QuickNotepad_1.14.java"));
        }

        @Test
        public void testSimpleApply00() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/simple/after.java"),
                                new File("src/test/resources/examples/simple/before.java"));
        }

        @Test
        public void testSimpleApplyOOB() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/vs/06b994/UtilityService/UtilityService_s.java"),
                                new File("src/test/resources/examples/vs/06b994/UtilityService/UtilityService_t.java"));
        }

        @Test
        public void testSimpleApply0() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/issue84_2/file_s.java"),
                                new File("src/test/resources/examples/issue84_2/file_t.java"));
                ApplyTestHelper.onChange(new File("src/test/resources/examples/issue84_2/file_t.java"),
                                new File("src/test/resources/examples/issue84_2/file_s.java"));
        }

        @Test
        public void testSimpleApply1() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_225724/left_ScarabRequestTool_1.36.java"),
                                new File("src/test/resources/examples/t_225724/right_ScarabRequestTool_1.37.java"));
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_225724/right_ScarabRequestTool_1.37.java"),
                                new File("src/test/resources/examples/t_225724/left_ScarabRequestTool_1.36.java"));
        }

        @Test
        public void testSimpleApply2() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_227005/left_AttributeValue_1.56.java"),
                                new File("src/test/resources/examples/t_227005/right_AttributeValue_1.57.java"));
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_227005/right_AttributeValue_1.57.java"),
                                new File("src/test/resources/examples/t_227005/left_AttributeValue_1.56.java"));
        }

        @Test
        public void testSimpleApply2shrinked() {
                String left = "class X {" + "NumberKey oldOptionId = null;" + "boolean oldOptionIdIsSet = false;"
                                + "    protected void setOptionIdOnly(NumberKey optionId)"
                                + "    throws TorqueException" + "{"
                                + "    if ( !Objects.equals(optionId, getOptionId()) )" + "    { "
                                + "        // if the value is set multiple times before saving only\n"
                                + "        // save the last saved value\n"
                                + "        if ( !isNew() && !oldOptionIdIsSet && getOptionId() != null) " + "        {"
                                + "            oldOptionId = new NumberKey(getOptionId());"
                                + "            oldOptionIdIsSet = true;" + "        }"
                                + "        super.setOptionId(optionId);" + "    }  " + "}}";
                String right = "class X {" + "NumberKey oldOptionId = null;" + "boolean oldOptionIdIsSet = false;"
                                + "protected void setOptionIdOnly(NumberKey optionId)"
                                + "        throws TorqueException" + "    {"
                                + "        if ( !Objects.equals(optionId, getOptionId()) )" + "        { "
                                + "            // if the value is set multiple times before saving only\n"
                                + "            // save the last saved value\n"
                                + "            if ( !isNew() && !oldOptionIdIsSet ) " + "            {"
                                + "                oldOptionId = new NumberKey(getOptionId());"
                                + "                oldOptionIdIsSet = true;" + "            }"
                                + "            super.setOptionId(optionId);" + "        }  " + "}}";
                // ApplyTestHelper.onChange(left,
                //         right);
                ApplyTestHelper.onChange(right, left);
        }

        @Test
        public void testSimpleApply2alt() {
                String left = "class X {" + "boolean r = !a && !b && c != null;" + "boolean a = true;"
                                + "boolean b = false;" + "Object c = null;" + "}";
                String right = "class X {" + "boolean r = !a && !b;" + "boolean a = true;" + "boolean b = false;" + "}";
                ApplyTestHelper.onChange(right, left);
        }

        @Test
        public void testSimpleApply2alt2() {
                String left = "class X {" + "boolean r = !a && !b && c != null;" + "boolean a = true;"
                                + "boolean b = false;" + "Object c = null;" + "}";
                String right = "class X {" + "boolean r = !b && c != null;" + "boolean a = true;" + "boolean b = false;"
                                + "Object c = null;" + "}";
                ApplyTestHelper.onChange(right, left);
        }

        @Test
        public void testSimpleApply3() {
                ApplyTestHelper.onChange(new File(
                                "src/test/resources/examples/t_204225/left_UMLModelElementStereotypeComboBoxModel_1.3.java"),
                                new File("src/test/resources/examples/t_204225/right_UMLModelElementStereotypeComboBoxModel_1.4.java"));
                ApplyTestHelper.onChange(new File(
                                "src/test/resources/examples/t_204225/right_UMLModelElementStereotypeComboBoxModel_1.4.java"),
                                new File("src/test/resources/examples/t_204225/left_UMLModelElementStereotypeComboBoxModel_1.3.java"));
        }

        @Test
        public void testSimpleApply4() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_211903/left_MemberFilePersister_1.4.java"),
                                new File("src/test/resources/examples/t_211903/right_MemberFilePersister_1.5.java"));
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_211903/right_MemberFilePersister_1.5.java"),
                                new File("src/test/resources/examples/t_211903/left_MemberFilePersister_1.4.java"));
        }

        @Test
        public void testSimpleApply5() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_222399/left_TdbFile_1.7.java"),
                                new File("src/test/resources/examples/t_222399/right_TdbFile_1.8.java"));
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_222399/right_TdbFile_1.8.java"),
                                new File("src/test/resources/examples/t_222399/left_TdbFile_1.7.java"));
        }

        @Test
        public void testSimpleApply6() {
                ApplyTestHelper.onChange(new File(
                                "src/test/resources/examples/919148/ReplicationRun/919148_ReplicationRun_0_s.java"),
                                new File("src/test/resources/examples/919148/ReplicationRun/919148_ReplicationRun_0_t.java"));
                ApplyTestHelper.onChange(new File(
                                "src/test/resources/examples/919148/ReplicationRun/919148_ReplicationRun_0_t.java"),
                                new File("src/test/resources/examples/919148/ReplicationRun/919148_ReplicationRun_0_s.java"));
        }

        @Test
        public void testSimpleApply7() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/test3/CommandLine1.java"),
                                new File("src/test/resources/examples/test3/CommandLine2.java"));
                ApplyTestHelper.onChange(new File("src/test/resources/examples/test3/CommandLine2.java"),
                                new File("src/test/resources/examples/test3/CommandLine1.java"));
        }

        @Test
        public void testSimpleApply8() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_222894/left_Client_1.150.java"),
                                new File("src/test/resources/examples/t_222894/right_Client_1.151.java"));
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_222894/right_Client_1.151.java"),
                                new File("src/test/resources/examples/t_222894/left_Client_1.150.java"));
        }

        @Test
        public void testSimpleApply9() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/test3/CommandLine2.java"),
                                new File("src/test/resources/examples/test3/TypeHandler2.java"));
        }

        @Test
        public void testSimpleApply10() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_223454/right_EntityListFile_1.18.java"),
                                new File("src/test/resources/examples/t_222399/left_TdbFile_1.7.java"));
        }

        @Test
        public void testSimpleApply11() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_221345/left_Server_1.187.java"),
                                new File("src/test/resources/examples/t_221422/right_Server_1.228.java"));
        }

        @Test
        public void testSimpleApply12() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_221422/right_Server_1.228.java"),
                                new File("src/test/resources/examples/t_221345/left_Server_1.187.java"));
        }

        @Test
        public void testSimpleApply12b() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/aaaa/right_Server_1.228.java"),
                                new File("src/test/resources/examples/aaaa/left_Server_1.187.java"));
        }
        @Test
        public void testSimpleApply13() {
                String left = "class X {" + "void f(){" + "int cen = Entity.NONE;" + "for(;;){};" + "}" + "}";
                String right = "class X {" + "void f(){" + "Vector results = new Vector(attacks.size());" + "for(;;){};"
                                + "int cen = Entity.NONE;" + "}" + "}";
                ApplyTestHelper.onChange(left, right);
        }

        @Test
        public void testSimpleApply13b() {
                String left = "class X {" +
                "}";

                // 1- Field@@TYPE_MEMBER
                // 1- LABEL@@tmp
                // 1- VARIABLE_TYPE@@TypeReference
                //         1- LABEL@@ArrayDeque
                // 1- NewClass@@DEFAULT_EXPRESSION

                String right = "import spoon.reflect.declaration.CtElement;import java.util.ArrayDeque;import java.util.Collection;import java.util.Iterator;" + 
                "class X {" + 
                "  java.util.ArrayDeque<spoon.reflect.declaration.CtClass> tmp = new java.util.ArrayDeque<spoon.reflect.declaration.CtClass>() {"+
                "      @java.lang.Override"+
                "      public boolean addAll(Collection c) {"+
                "          for (Object aC : c) {"+
                "              this.addFirst((spoon.reflect.declaration.CtClass) (aC));"+
                "          }"+
                "          return !c.isEmpty();"+
                "      }"+
                "  };" + 
                "}";

                ApplyTestHelper.onChange(left, right);
        }

        

        @Test
        public void testSimpleApply14b() {
                // 1- Field@@TYPE_MEMBER
                // 1- LABEL@@deque
                // 1- VARIABLE_TYPE@@TypeReference
                //         1- LABEL@@ArrayDeque
                //         1- TypeReference@@TypeReference
                //                 1- LABEL@@CtElement
                // 1- MODIFIER@@private
                // 1- NewClass@@DEFAULT_EXPRESSION
                ApplyTestHelper.onChange(new File("src/test/resources/examples/simple2/before.java"),
                                new File("src/test/resources/examples/simple2/after.java"));
        }

        @Test
        public void testSimpleApply14c() {
                // 1- Field@@TYPE_MEMBER
                // 1- LABEL@@deque
                // 1- VARIABLE_TYPE@@TypeReference
                //         1- LABEL@@ArrayDeque
                //         1- TypeReference@@TypeReference
                //                 1- LABEL@@CtElement
                // 1- MODIFIER@@private
                // 1- NewClass@@DEFAULT_EXPRESSION
                ApplyTestHelper.onChange(new File("src/test/resources/examples/simple3/before.java"),
                                new File("src/test/resources/examples/simple3/after.java"));
        }
        

        @Test
        public void testSimpleApply14() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/more/right_Server_1.228.java"),
                                new File("src/test/resources/examples/more/left_Server_1.187.java"));
        }

        @Test
        public void testSimpleApply15() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_223454/right_EntityListFile_1.18.java"),
                                new File("src/test/resources/examples/t_222399/left_TdbFile_1.7.java"));
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_222399/left_TdbFile_1.7.java"),
                                new File("src/test/resources/examples/t_223454/right_EntityListFile_1.18.java"));
        }

        @Test
        public void testSimpleApply16() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_225414/left_IndexWriter_1.41.java"),
                                new File("src/test/resources/examples/919148/ReplicationRun/919148_ReplicationRun_0_s.java"));
        }

        @Test
        public void testSimpleApply17() {// TODO one of the most complicated
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_208618/left_PropPanelUseCase_1.39.java"),
                                new File("src/test/resources/examples/919148/ReplicationRun/919148_ReplicationRun_0_t.java"));
        }
        @Test
        public void testSimpleApply18() {
                ApplyTestHelper.onChange(new File("src/test/resources/examples/t_222894/right_Client_1.151.java"),
                                new File("src/test/resources/examples/t_224766/right_SegmentTermEnum_1.2.java"));
        }

        @Test
        public void testSimpleApply18Reversed() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_224766/right_SegmentTermEnum_1.2.java"),
                                new File("src/test/resources/examples/t_222894/right_Client_1.151.java"));
        }
        @Test
        public void testSimpleApply19() {
                String left = "abstract class X {"
                + "}";
                String right = "@A abstract class X <T extends a.b.Y, U> {"
                + "}";
                ApplyTestHelper.onChange(left, right);
        }
        @Test
        public void testSimpleApply20() {
                String left = "class X {"
                + "}";
                String right = "enum Y {"
                + "AAA(),BBB();"
                + "class X {}"
                + "}";
                ApplyTestHelper.onChange(left, right);
        }
        @Test
        public void testSimpleApply20b() {
                String left = "enum Y {"
                + "}";
                String right = "enum Y {"
                + "AAA,"
                + "BBB();"
                + "Y(){}}";
                ApplyTestHelper.onChange(left, right);
        }
        @Test
        public void testSimpleApply21() {
                String left = "class X {"
                + "}";
                String right = "class X {"
                + "Object f = (int i) -> i > 0;"
                + "}";
                ApplyTestHelper.onChange(left, right);
        }
        @Test
        public void testSimpleApply22() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/test8/left.java"),
                                new File("src/test/resources/examples/test8/right.java"));
        }
        @Test
        public void testSimpleApply23() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/test8/right.java"),
                                new File("src/test/resources/examples/test8/left.java"));
        }
        @Test
        public void testSimpleApply24() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/test9/left.java"),
                                new File("src/test/resources/examples/test9/right.java"));
        }
        @Test
        public void testSimpleApply25() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/test9/right.java"),
                                new File("src/test/resources/examples/test9/left.java"));
        }
        @Test
        public void testSimpleApply26() {
                ApplyTestHelper.onChange(
                                new File("src/test/resources/examples/t_224890/left_DateField_1.4.java"),
                                new File("src/test/resources/examples/t_224890/right_DateField_1.5.java"));
        }

        @Test
        public void testSimpleApply27() {
                String left = "class Y {}";
                String right = "class Y { public Y() {} void f() throws java.lang.Exception {} }";
                ApplyTestHelper.onChange(left, right);
        }
        
        @Test
        public void testSimpleApply28() {
                String left = "public class X {"
                + "} class Y {}";
                String right = "public class X extends Y.Z {"
                + "} class Y { public class Z {} }";
                ApplyTestHelper.onChange(left, right);
        }

        @Test
        public void testSimpleApply29() {
                String contentsLeft = "class X {void f() { while(false) {continue;}}}";
                String contentsRight = "class X {void f() {while(false) {break;}}}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }

        @Test
        public void testSimpleApply30() {
                String contentsLeft = "public class X { String f(String s){return s=\"Hello World!\";}}";
                String contentsRight = "public class X { String f(String s){return s=\"Hello!\";}}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }

        @Test
        public void testSimpleApply31() {
                String contentsLeft = "public class X { String f(String s){return s=\"Hello World!\";}}";
                String contentsRight = "public class X { String f(String s){int i = (-3); ++i; return s=\"Hello!\";}}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }

        @Test
        public void testSimpleApply32() {//TODO 17
                String contentsLeft = "public class X {}";
                String contentsRight = "package org.apache.derbyTesting.functionTests.tests.replicationTests;"+
                "import org.apache.derbyTesting.functionTests.tests.replicationTests.ReplicationRun;"+
                "import org.apache.derbyTesting.junit.BaseTestCase;"+
                "public class ReplicationRun extends BaseTestCase { public ReplicationRun(java.lang.String testcaseName) {super(testcaseName);ReplicationRun.LF = java.lang.System.getProperties().getProperty(\"line.separator\");}}";
                ApplyTestHelper.onChange(contentsLeft, contentsRight);
        }
        
}