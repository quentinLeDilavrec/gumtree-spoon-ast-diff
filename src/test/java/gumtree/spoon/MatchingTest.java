package gumtree.spoon;

import java.io.File;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionInt;
import com.github.gumtreediff.tree.Version.COMP_RES;

import org.junit.Before;
import org.junit.Test;

import gumtree.spoon.apply.AAction;
import gumtree.spoon.apply.ActionApplier;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import spoon.MavenLauncher;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;

public class MatchingTest {

        // @Before
        // public void initProps() {
        //         System.setProperty("nolabel", "true");
        // }

        @Test
        public void testSimpleApplyInsertAssignCall3() {
                String contentsLeft = "class X {int f(){return 0;}}";
                String contentsRight = "class X {int g(){return 0;}}";
                Factory left = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java"));
                Factory right = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java"));
                CtElement right1 = right.getModel().getRootPackage();
                Environment env = new StandardEnvironment();
                spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);
                final Version leftV = new VersionInt(0);
                final VersionInt rightV = new VersionInt(1);
                final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
                ITree srcTree;
                srcTree = scanner.getTree(left.getModel().getRootPackage());
                MultiDiffImpl mdiff = new MultiDiffImpl(srcTree, leftV);
                ITree dstTree = scanner.getTree(right1);
                DiffImpl diff = mdiff.compute(scanner.getTreeContext(), dstTree, rightV);

                ITree middle = mdiff.getMiddle();
                // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), srcTree));
                // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
                // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), middle));
                for (Action action : diff.getAtomicActions()) {
                        try {
                                if (action instanceof Insert) {
                                        ActionApplier.applyAInsert((Factory) middle.getMetadata("Factory"),
                                                        scanner.getTreeContext(), (Insert & AAction<Insert>) action);
                                } else if (action instanceof Delete) {
                                        ActionApplier.applyADelete((Factory) middle.getMetadata("Factory"),
                                                        scanner.getTreeContext(), (Delete & AAction<Delete>) action);
                                } else if (action instanceof Update) {
                                        ActionApplier.applyAUpdate((Factory) middle.getMetadata("Factory"),
                                                        scanner.getTreeContext(), (Update & AAction<Update>) action);
                                } else if (action instanceof Move) {
                                        ActionApplier.applyAMove((Factory) middle.getMetadata("Factory"),
                                                        scanner.getTreeContext(), (Move & AAction<Move>) action);
                                } else {
                                        throw null;
                                }
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                }
                CtElement middleE = null;
                Queue<ITree> tmp = new LinkedBlockingDeque<>();
                tmp.add(middle);

                while (middleE == null) {
                        ITree curr = tmp.poll();
                        middleE = (CtElement) curr.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                        List<ITree> children = curr.getChildren();
                        tmp.addAll(children);
                }
                
                
                System.out.println(middleE);

        }

        // @Test
        // public void testSimpleApplySuper2() {
        //         ApplyTestHelper.onChange(
        //                         new File("src/test/resources/examples/roots/test9/left_QuickNotepad_1.13.java"),
        //                         new File("src/test/resources/examples/roots/test9/right_QuickNotepad_1.14.java"));
        // }

        
}