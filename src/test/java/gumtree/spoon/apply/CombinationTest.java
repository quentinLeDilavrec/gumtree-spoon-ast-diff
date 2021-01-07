package gumtree.spoon.apply;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.actions.MyAction.MyDelete;
import com.github.gumtreediff.actions.MyAction.MyInsert;
import com.github.gumtreediff.actions.MyAction.MyMove;
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
import com.github.gumtreediff.tree.VersionedTree;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.notification.Failure;

import gumtree.spoon.apply.ActionApplier;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.apply.Flattener.Clusterizer.Cluster;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import spoon.MavenLauncher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtReference;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;

public class CombinationTest {
    private static final class VirtComposedAction implements ComposedAction<AbstractVersionedTree> {
        final Collection<MyAction<?>> wanted;

        private VirtComposedAction(Collection<MyAction<?>> wanted) {
            this.wanted = wanted;
        }

        @Override
        public String getName() {
            throw null;
        }

        @Override
        public List<? extends MyAction<?>> composed() {
            return new ArrayList<>(wanted);
        }

        @Override
        public String toString() {
            StringBuilder r = new StringBuilder();
            r.append(hashCode() + " Virt action containing the " + composed().size() + " following actions:");
            for (MyAction<?> component : composed()) {
                for (String str : component.toString().split("\n")) {
                    r.append("\n\t");
                    r.append(str);
                }
            }
            return r.toString();
        }
    }

    @Test
    public void testSimpleApply() {
        String contentsLeft = "class X { " + "}";
        String contentsRight = "interface X { " + "}";
        aaaa(contentsLeft, contentsRight);
    }

    @Test
    public void testSimpleApply1() {
        String contentsLeft = "class X { void f(){} void g(){}}";
        String contentsRight = "class X { class Y {void g(){}} void f(){}}";
        aaaa(contentsLeft, contentsRight);
    }

    @Test
    public void testSimpleApply2() {
        String contentsLeft = "class X { void f(){} void g(){}}";
        String contentsRight = "class X { class Y {void g(){} void h(){}} void f(){}}";
        aaaa(contentsLeft, contentsRight);
    }

    @Test
    public void testSimpleApply3() {
        String contentsLeft = "public class X { static void f(){} static void g(){}}";
        String contentsLeftT = "public class XTest { @Test void fTest(){X.f();} @Test void gTest(){X.g();}}";
        String contentsRight = "public class X { public static class Y {static void g(){} static void h(){}} static void f(){}}";
        String contentsRightT = "public class XTest { @Test void fTest(){X.f();} @Test void gTest(){X.Y.g();} @Test void hTest(){X.Y.h();}}";
        Factory left = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java"),
                new VirtualFile(contentsLeftT, "XTest.java"));
        for (CompilationUnit cu : left.CompilationUnit().getMap().values()) {
            if (cu.getFile().getName().contains("Test")) {
                cu.putMetadata("isTest", true);
            }
        }
        Factory right = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java"),
                new VirtualFile(contentsRightT, "XTest.java"));
        for (CompilationUnit cu : right.CompilationUnit().getMap().values()) {
            if (cu.getFile().getName().contains("Test")) {
                cu.putMetadata("isTest", true);
            }
        }
        aaaa(left.getModel().getRootPackage(), right.getModel().getRootPackage());
    }

    @Test
    public void testSimpleApply4() {
        // TODO look at the @org.junit.Test, gt thinks that it is moved to hTest :/
        String contentsLeft = "public class X { static void f(){} static void g(){}}";
        String contentsLeftT = "public class XTest { @Test void fTest(){X.f();} @Test void gTest(){X.g();}}";
        String contentsRight = "public class X { public static class Y {static void g(){} static void h(){}} static void f(){}}";
        String contentsRightT = "public class XTest { String s = \"\"; @org.junit.Test void fTest(){X.f();} @Test void gTest(){X.Y.g();} @Test void hTest(){X.Y.h();}}";
        Factory left = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java"),
                new VirtualFile(contentsLeftT, "XTest.java"));
        for (CompilationUnit cu : left.CompilationUnit().getMap().values()) {
            if (cu.getFile().getName().contains("Test")) {
                cu.putMetadata("isTest", true);
            }
        }
        Factory right = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java"),
                new VirtualFile(contentsRightT, "XTest.java"));
        for (CompilationUnit cu : right.CompilationUnit().getMap().values()) {
            if (cu.getFile().getName().contains("Test")) {
                cu.putMetadata("isTest", true);
            }
        }
        aaaa(left.getModel().getRootPackage(), right.getModel().getRootPackage());
    }
    // @Test
    // public void testSimpleApply4() {
    //     String contentsLeft = "public class X { int i; public static class Y {static void g(){} static void h(){}} static void f(){}}";
    //     String contentsLeftT = "public class XTest { @Test void fTest(){X.f();} @Test void gTest(){X.Y.g();} @Test void hTest(){X.Y.h();}}";
    //     String contentsRight = "public class X { static void f(){} static void g(){}}";
    //     String contentsRightT = "public class XTest { @Test void fTest(){X.f();} @Test void gTest(){X.g();}}";
    //     Factory left = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java"),new VirtualFile(contentsLeftT, "XTest.java"));
    //     Factory right = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java"),new VirtualFile(contentsRightT, "XTest.java"));
    //     aaaa(left.getModel().getRootPackage(), right.getModel().getRootPackage());
    // }

    public static void aaaa(String contentsLeft, String contentsRight) {
        Factory left = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java"));
        Factory right = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java"));
        aaaa(left.getModel().getRootPackage(), right.getModel().getRootPackage());
    }

    public static void aaaa(File contentsLeft, File contentsRight) {
        Factory left = MyUtils.makeFactory(contentsLeft);
        Factory right = MyUtils.makeFactory(contentsRight);
        aaaa(left.getModel().getRootPackage(), right.getModel().getRootPackage());
    }

    public static void aaaa(CtElement left, CtElement right) {
        Environment env = new StandardEnvironment();
        spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);
        final Version leftV = new VersionInt(0);
        final Version rightV = new VersionInt(1);
        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        ITree srcTree;
        srcTree = scanner.getTree(left);
        MultiDiffImpl mdiff = new MultiDiffImpl(srcTree, leftV);
        ITree dstTree = scanner.getTree(right);
        DiffImpl diff = mdiff.compute(dstTree, rightV);

        AbstractVersionedTree middle = mdiff.getMiddle();
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), srcTree));
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), middle));
        CtElement middleE = ((Factory) middle.getMetadata("Factory")).getModel().getRootPackage();
        Collection<MyAction<?>> wanted = new HashSet<>();
        wanted.addAll(diff.getComposed());
        wanted.addAll(diff.getAtomic());
        Collection<MyAction<?>> testSet = new HashSet<>();
        Collection<MyAction<?>> appSet = new HashSet<>();
        for (MyAction<?> a : wanted) {
            if (isInTest(a)) {
                testSet.add(a);
            } else if (isInApp(a)) {
                appSet.add(a);
            }
        }
        Set<AtomicAction<AbstractVersionedTree>> wanted2 = new HashSet<>();
        Map<AtomicAction<AbstractVersionedTree>, Set<ComposedAction<AbstractVersionedTree>>> composed = new HashMap<>();
        Set<VirtComposedAction> projectWide = new HashSet<>();
        if (!testSet.isEmpty() && !appSet.isEmpty()) {
            projectWide.add(new VirtComposedAction(testSet));
            projectWide.add(new VirtComposedAction(appSet));
        } else {
            projectWide.add(new VirtComposedAction(wanted));
        }
        wanted.addAll(projectWide);
        for (VirtComposedAction virtComposedAction : projectWide) {
            Combination.atomize(virtComposedAction, wanted2, composed);
        }
        // Collection<MyAction<?>> topAto = new HashSet<>();
        // for (MyAction<?> a : wanted) {
        //     if (a instanceof AtomicAction && composed.get(a).stream().allMatch(x -> x instanceof VirtComposedAction)) {
        //         VirtComposedAction o = new VirtComposedAction(Collections.singleton(a));
        //         for (ComposedAction<AbstractVersionedTree> aa : composed.get(a)) {
        //             ((VirtComposedAction)aa).wanted.add(o);
        //         }
        //         composed.get((AtomicAction<AbstractVersionedTree>) a).add(o);
        //         topAto.add(o);
        //     }
        // }
        // wanted.addAll(topAto);
        Flattener.ComposingClusterizer flat = new Flattener.ComposingClusterizer(new Flattener.Clusterizer(wanted2) ,wanted2);
        Set<ComposedAction<AbstractVersionedTree>> composed2 = new HashSet<>();
        for (Set<ComposedAction<AbstractVersionedTree>> ca : composed.values()) {
            composed2.addAll(ca);
        }
        for (ComposedAction<AbstractVersionedTree> ca : composed2) {
            flat.clusterize(ca);
        }
        // for (ImmutablePair<Integer, AbstractVersionedTree> a : flat.getConstrainedTree()) {
        //     System.out.println(a.right);
        // }
        Set<Cluster> projectWide2 = new HashSet<>();
        projectWide.stream().forEach(x->projectWide2.addAll(flat.getCluster(x)));
        flat.setInibiteds(projectWide2);
        List<ImmutablePair<Integer, Cluster>> l = flat.getConstrainedTree();
        int[] init = l.stream().map(x->flat.getActions().contains(x.right.getRoot().getMetadata(MyScriptGenerator.DELETE_ACTION))?1:0).mapToInt(Integer::intValue).toArray();//Combination.initialState(l);
        int[] leafs = Combination.detectLeafs(l);
        Cluster[] nodes = l.stream().map(ImmutablePair::getRight).toArray(Cluster[]::new);
        int[] deps = l.stream().map(ImmutablePair::getLeft).mapToInt(Integer::intValue).toArray();
        for (ImmutablePair<Integer, Cluster> a : l) {
            System.out.println(a.right);
        }

    }

    private static boolean isInApp(MyAction<?> a) {
        if (a instanceof AtomicAction) {
            AbstractVersionedTree tree = ((AtomicAction<AbstractVersionedTree>) a).getTarget();
            assert tree != null;
            while (tree != null) {
                CtElement ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
                if (ele == null) {
                    tree = tree.getParent();
                    continue;
                }
                if (ele instanceof CtReference) {
                    tree = tree.getParent();
                    continue;
                }
                SourcePosition pos = ele.getPosition();
                if (pos == null) {
                    tree = tree.getParent();
                    continue;
                }
                CompilationUnit cu = pos.getCompilationUnit();
                if (cu == null) {
                    tree = tree.getParent();
                    continue;
                }
                Boolean isTest = (Boolean) cu.getMetadata("isTest");
                if (isTest != null && isTest) {
                    return false;
                } else {
                    return true;
                }
            }
        } else if (a instanceof ComposedAction) {
            for (MyAction<?> aa : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                if (!isInTest(aa)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isInTest(MyAction<?> a) {
        if (a instanceof AtomicAction) {
            AbstractVersionedTree tree = ((AtomicAction<AbstractVersionedTree>) a).getTarget();
            assert tree != null;
            while (tree != null) {
                CtElement ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
                if (ele == null) {
                    tree = tree.getParent();
                    continue;
                }
                if (ele instanceof CtReference) {
                    tree = tree.getParent();
                    continue;
                }
                SourcePosition pos = ele.getPosition();
                if (pos == null) {
                    tree = tree.getParent();
                    continue;
                }
                CompilationUnit cu = pos.getCompilationUnit();
                if (cu == null) {
                    tree = tree.getParent();
                    continue;
                }
                Boolean isTest = (Boolean) cu.getMetadata("isTest");
                if (isTest != null && isTest) {
                    return true;
                } else {
                    return false;
                }
            }
        } else if (a instanceof ComposedAction) {
            for (MyAction<?> aa : ((ComposedAction<AbstractVersionedTree>) a).composed()) {
                if (!isInTest(aa)) {
                    return false;
                }
            }
        }
        return true;
    }
}
