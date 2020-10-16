package gumtree.spoon;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import gumtree.spoon.apply.MyUtils;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.VirtualFile;

public class InsertApplyTest {
    @Before
    public void initProps() {
        System.setProperty("nolabel", "true");
    }

    @Test
    public void testSimpleApplyInsertAssignCall3() {
        String contents = "class X { "
                + "private ServerSocket serverSocket;"
                + "public X(java.lang.String password, int port) {"
                // + "this.password = (password.length() > 0) ? password : null;" 
                + "try {"
                + "    serverSocket = new java.net.ServerSocket(port);" 
                + "} catch (java.io.IOException ex) {" 
                + "}"
                // + "motd = createMotd();" 
                // + "game.getOptions().initialize();" 
                + "}}";
        ApplyTestHelper.onInsert(contents);
    }
    @Test
    public void testSimpleApplyInsertCall() {
        String contents = "class X { "
                + "private Game game = new Game();"
                + "public X(java.lang.String password, int port) {"
                // + "this.password = (password.length() > 0) ? password : null;" 
                + "changePhase(Game.PHASE_LOUNGE);"
                + "}" 
                + "private void changePhase(int 0){}" 
                + "}";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertCast() {
        String contents = "class X { "
                + "private Hashtable commandsHash = new Hashtable();"
                + "public ServerCommand getCommand(String name) {"
                + "return (ServerCommand)commandsHash.get(name);"
                + "}"
                + "}";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertJavax() {
        String contents = "import javax.swing.JPanel;"
                + "public class X extends JPanel implements Y {"
                + "public X() {"
                + "super();"
                + "}"
                + "}";
        ApplyTestHelper.onInsert(contents);
    }

    @Test
    public void testSimpleApplyInsertNoSup() {
        String contents = "import javax.swing.JPanel;"
                + "public class X extends JPanel {"
                + "public X() {"
                + "}"
                + "}";
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
    public void testSimpleApplyInsertSuper2() {
        ApplyTestHelper.onInsert(new File("src/test/resources/examples/roots/test9/right_QuickNotepad_1.14.java"));
    }

    @Test
    public void testSimpleApplyInsertOOB() {
        ApplyTestHelper.onInsert(new File("src/test/resources/examples/vs/06b994/UtilityService/UtilityService_t.java"));
    }
    
}