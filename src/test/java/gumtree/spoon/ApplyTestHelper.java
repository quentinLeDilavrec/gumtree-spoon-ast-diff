package gumtree.spoon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
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

import org.apache.commons.lang3.tuple.MutablePair;
import org.junit.Assert;
import org.junit.runner.notification.Failure;

import gumtree.spoon.apply.ActionApplier;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import spoon.MavenLauncher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;

public class ApplyTestHelper {
    public static void onInsert(CtElement right) {
        Environment env = new StandardEnvironment();
        spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);
        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        CtElement left = null;
        ITree srctree;
        srctree = scanner.getTree(left);
        final Version leftV = new VersionInt(0);
        final Version rightV = new VersionInt(1);
        MultiDiffImpl mdiff = new MultiDiffImpl(srctree, leftV);
        ITree dstTree = scanner.getTree(right);
        DiffImpl diff = mdiff.compute(dstTree, rightV);

        ITree middle = mdiff.getMiddle();
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), middle));
        for (Action action : diff.getAtomic()) {
            try {
                ActionApplier.applyMyInsert((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyInsert) action);
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
        if (right instanceof CtType || right instanceof CtPackage) {
            CtPackage made1 = MyUtils.makeFactory(toVirtFiles(pp, middleE)).getModel().getRootPackage();
            CtPackage ori1 = MyUtils.makeFactory(toVirtFiles(pp, right)).getModel().getRootPackage();
            if (!ori1.equals(made1)) {
                final SpoonGumTreeBuilder scanner1 = new SpoonGumTreeBuilder();
                ITree srctree1;
                srctree1 = scanner1.getTree(made1);
                MultiDiffImpl mdiff1 = new MultiDiffImpl(srctree1, leftV);
                ITree dstTree1 = scanner1.getTree(ori1);
                DiffImpl diff1 = mdiff1.compute(dstTree1, rightV);
                for (Action action : diff1.getAtomic()) {
                    System.err.println(action);
                }
                check1(right, pp, middleE);
            }
        } else {
            check1(right, pp, middleE);
        }
    }

    private static void onDelete(CtElement left) {
        Environment env = new StandardEnvironment();
        spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);
        final Version leftV = new VersionInt(0);
        final Version rightV = new VersionInt(1);
        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        CtElement right = null;
        ITree dstTree;
        dstTree = scanner.getTree(right);
        MultiDiffImpl mdiff = new MultiDiffImpl(dstTree, leftV);
        ITree srcTree = scanner.getTree(left);
        DiffImpl diff = mdiff.compute(dstTree, rightV);

        ITree middle = mdiff.getMiddle();
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
        System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), middle));
        for (Action action : diff.getAtomic()) {
            try {
                ActionApplier.applyMyDelete((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyDelete) action);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        CtElement middleE = null;
        Queue<ITree> tmp = new LinkedBlockingDeque<>();
        tmp.add(middle);
        CtModel aaa = ((Factory) middle.getMetadata("Factory")).getModel();
        assertEquals(aaa.getAllTypes().size(), 0);
    }

    private static final class Version1 implements Version {
        public Version other;

        @Override
        public COMP_RES partiallyCompareTo(Version other) {
            if (other == this) {
                return Version.COMP_RES.EQUAL;
            }
            return other == this ? Version.COMP_RES.EQUAL
                    : (other == this.other ? Version.COMP_RES.INFERIOR : Version.COMP_RES.UNKNOWN);
        }
    }

    private static VirtualFile[] toVirtFiles(PrettyPrinter pp, CtElement ele) {
        List<VirtualFile> l = new ArrayList<>();
        if (ele instanceof CtType) {
            l.add(new VirtualFile("q" + new Random().nextInt(100), pp.prettyprint(ele)));
        } else {
            for (CtType p : ((CtPackage) ele).getTypes()) {
                if (!p.isShadow()) {
                    l.add(new VirtualFile("q" + new Random().nextInt(100), pp.prettyprint(p)));
                }
            }
        }
        return l.toArray(new VirtualFile[l.size()]);
    }

    private static void check1(CtElement right, spoon.reflect.visitor.PrettyPrinter pp, CtElement middleE) {
        HashMap<String, MutablePair<CtType, CtType>> res = new HashMap<>();
        synchro(pp, right, middleE, res);
        for (MutablePair<CtType, CtType> p : res.values()) {
            System.err.println(p.right.getClass());
            System.err.println("*" + p.right.getQualifiedName() + "*");
            try {
                System.err.println(pp.prettyprint(p.left));
            System.err.println("--------------->>");
            System.err.println(pp.prettyprint(p.right));
            } catch (Exception e) {
                assumeNoException(e);
            }
        }
        for (MutablePair<CtType, CtType> p : res.values()) {
            assertEquals(pp.prettyprint(p.left), pp.prettyprint(p.right));
        }
    }

    private static void synchro(PrettyPrinter pp, CtElement right, CtElement middle,
            Map<String, MutablePair<CtType, CtType>> res) {
        if (right instanceof CtType) {
            assertTrue(middle instanceof CtType);
            res.put(((CtType) right).getQualifiedName(), new MutablePair(right, middle));
        } else if (right instanceof CtPackage) {
            assertTrue(middle.getClass().toString(), middle instanceof CtPackage);
            Map<String, MutablePair<CtType, CtType>> m = new HashMap<>();
            for (CtType<?> t : ((CtPackage) right).getTypes()) {
                m.put(t.getQualifiedName(), new MutablePair(t, null));
            }
            for (CtType<?> t : ((CtPackage) middle).getTypes()) {
                if (!t.isShadow()) {
                    m.get(t.getQualifiedName()).setRight(t);
                }
            }
            for (MutablePair<CtType, CtType> p : m.values()) {
                assertNotNull(p.left);
                assertNotNull(p.right);
                synchro(pp, p.left, p.right, res);
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
                synchro(pp, p.left, p.right, res);
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

    public static void onDelete(String contents) {
        Factory right = MyUtils.makeFactory(new VirtualFile(contents, "X.java"));
        onDelete(right.getModel().getRootPackage());
    }

    public static void onDelete(File... contents) {
        Factory right = MyUtils.makeFactory(contents);
        onDelete(right.getModel().getRootPackage());
    }

    public static void onChange(String contentsLeft, String contentsRight) {
        Factory left = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java"));
        Factory right = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java"));
        onChange(left.getModel().getRootPackage(), right.getModel().getRootPackage());
    }

    public static void onChange(File contentsLeft, File contentsRight) {
        Factory left = MyUtils.makeFactory(contentsLeft);
        Factory right = MyUtils.makeFactory(contentsRight);
        onChange(left.getModel().getRootPackage(), right.getModel().getRootPackage());
    }

    public static void onChange(CtElement left, CtElement right) {
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
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), srcTree));
        // System.out.println(MyUtils.toPrettyTree(scanner.getTreeContext(), dstTree));
        System.out.println(middle.toTreeString());
        CtElement middleE = ((Factory)middle.getMetadata("Factory")).getModel().getRootPackage();
        check1(left, pp, middleE);
        List<Action> wanted = (List)diff.getAtomic();
        for (Action action : wanted) {
            applyAux(scanner, middle, action, new HashSet<>((Collection)wanted));
        }
        middleE = ((Factory)middle.getMetadata("Factory")).getModel().getRootPackage();
        // Queue<ITree> tmp = new LinkedBlockingDeque<>();
        // tmp.add(middle);

        // while (middleE == null) {
        //     ITree curr = tmp.poll();
        //     middleE = (CtElement) curr.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        //     List<ITree> children = curr.getChildren();
        //     tmp.addAll(children);
        // }
        // pp.prettyprint(((CtRootPackage)srcTree.getChild(0).getMetadata("spoon_object")).getTypes().iterator().next())
        if (right instanceof CtType || right instanceof CtPackage) {
            try {
                CtPackage made1 = MyUtils.makeFactory(toVirtFiles(pp, middleE)).getModel().getRootPackage();
                CtPackage ori1 = MyUtils.makeFactory(toVirtFiles(pp, right)).getModel().getRootPackage();
                if (!ori1.equals(made1)) {
                        final SpoonGumTreeBuilder scanner1 = new SpoonGumTreeBuilder();
                        ITree srctree1;
                        srctree1 = scanner1.getTree(made1);
                        MultiDiffImpl mdiff1 = new MultiDiffImpl(srctree1, leftV);
                        ITree dstTree1 = scanner1.getTree(ori1);
                        DiffImpl diff1 = mdiff1.compute(dstTree1, rightV);
                        try {
                            check1(right, pp, middleE);
                        } catch (Throwable e) {
                            for (Action action : diff1.getAtomic()) {
                                e.addSuppressed(new AssertionError(action));
                            }
                        }
                }
            } catch (Exception e) {
                check1(right, pp, middleE);
            }
        } else {
            check1(right, pp, middleE);
        }
    }
    

    public static void applyAux(final SpoonGumTreeBuilder scanner, ITree middle, Action action, Collection<MyAction<?>> wanted) {
        try {
            if (action instanceof MyInsert) {
                ActionApplier.applyMyInsert((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyInsert) action);
                for (AtomicAction<AbstractVersionedTree> a : ((MyInsert) action).getCompressed()) {
                    if (!wanted.contains(a)) {
                        applyAux(scanner, middle, (Action)a, wanted);
                    }
                }
            } else if (action instanceof MyDelete) {
                for (AtomicAction<AbstractVersionedTree> a : ((MyDelete) action).getCompressed()) {
                    if (!wanted.contains(a)) {
                        applyAux(scanner, middle, (Action)a, wanted);
                    }
                }
                ActionApplier.applyMyDelete((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyDelete) action);
            } else if (action instanceof MyUpdate) {
                ActionApplier.applyMyUpdate((Factory) middle.getMetadata("Factory"), scanner.getTreeContext(),
                        (MyUpdate) action);
                for (AtomicAction<AbstractVersionedTree> a : ((MyUpdate) action).getCompressed()) {
                    if (!wanted.contains(a)) {
                        applyAux(scanner, middle, (Action)a, wanted);
                    }
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
}