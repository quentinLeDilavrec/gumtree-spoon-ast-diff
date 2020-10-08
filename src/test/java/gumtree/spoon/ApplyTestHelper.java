package gumtree.spoon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.tree.ITree;

import org.apache.commons.lang3.tuple.MutablePair;

import gumtree.spoon.apply.AAction;
import gumtree.spoon.apply.ActionApplier;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;

public class ApplyTestHelper {
    public static void onInsert(CtElement right) {
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
            ActionApplier.applyAInsert((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
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

    private static void compare(PrettyPrinter pp, CtElement right, CtElement middle,
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
    public static void onInsert(String contents) {
        Factory right = MyUtils.makeFactory(new VirtualFile(contents, "X.java"));
        onInsert(right.getModel().getRootPackage());
    }
    public static void onInsert(File... contents) {
        Factory right = MyUtils.makeFactory(contents);
        onInsert(right.getModel().getRootPackage());
    }
}