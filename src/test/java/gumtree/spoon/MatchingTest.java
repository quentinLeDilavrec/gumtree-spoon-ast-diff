package gumtree.spoon;

import java.io.File;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.MyDelete;
import com.github.gumtreediff.actions.MyAction.MyInsert;
import com.github.gumtreediff.actions.MyAction.MyUpdate;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionInt;
import com.github.gumtreediff.tree.Version.COMP_RES;

import org.junit.Before;
import org.junit.Test;

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
    //   System.setProperty("nolabel", "true");
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
        DiffImpl diff = mdiff.compute(dstTree, rightV);

        ITree middle = mdiff.getMiddle();
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), srcTree));
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), middle));
        for (Action action : diff.getAtomic()) {
            applyAux(scanner, middle, action);
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

    public static void applyAux(final SpoonGumTreeBuilder scanner, ITree middle, Action action) {
        try {
            if (action instanceof MyInsert) {
                ActionApplier.applyMyInsert((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyInsert) action);
                for (AtomicAction<AbstractVersionedTree> a : ((MyInsert) action).getCompressed()) {
                    applyAux(scanner, middle, (Action)a);
                }
            } else if (action instanceof MyDelete) {
                for (AtomicAction<AbstractVersionedTree> a : ((MyDelete) action).getCompressed()) {
                    applyAux(scanner, middle, (Action)a);
                }
                ActionApplier.applyMyDelete((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyDelete) action);
            } else if (action instanceof MyUpdate) {
                ActionApplier.applyMyUpdate((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyUpdate) action);
                for (AtomicAction<AbstractVersionedTree> a : ((MyUpdate) action).getCompressed()) {
                    applyAux(scanner, middle, (Action)a);
                }
                // } else if (action instanceof MyMove) {
                //     ActionApplier.applyMyMove((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                //       (MyMove) action);
            } else {
                throw new RuntimeException(action.getClass().toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // @Test
    // public void testSimpleApplySuper2() {
    //   ApplyTestHelper.onChange(
    //       new File("src/test/resources/examples/roots/test9/left_QuickNotepad_1.13.java"),
    //       new File("src/test/resources/examples/roots/test9/right_QuickNotepad_1.14.java"));
    // }

}