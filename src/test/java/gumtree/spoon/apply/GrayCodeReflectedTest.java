package gumtree.spoon.apply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.AbstractTree.FakeTree;
import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionInt;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import gumtree.spoon.apply.Combination;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.apply.Combination.CombinationHelper;
import gumtree.spoon.apply.Combination.ReflectedConstrainedHelper;
import gumtree.spoon.apply.Flattener.Clusterizer.Cluster;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import gumtree.spoon.diff.operations.Operation;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;

public class GrayCodeReflectedTest {

    @Test
    public void treeTest() throws MultipleConstraintsException {
        String contentsLeft = "class X {void f(){} void g(){f();} void h(){g();}}";
        String contentsRight = "class X {class Y{void g(){f();}}}";
        Factory leftF = MyUtils.makeFactory(new VirtualFile(contentsLeft, "X.java"));
        Factory rightF = MyUtils.makeFactory(new VirtualFile(contentsRight, "X.java"));
        CtPackage left = leftF.getModel().getRootPackage();
        CtPackage right = rightF.getModel().getRootPackage();
        Environment env = new StandardEnvironment();
        spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);
        final Version rightV = new VersionInt(1);
        final Version leftV = new VersionInt(0);
        final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
        ITree srcTree;
        srcTree = scanner.getTree(left);
        MultiDiffImpl mdiff = new MultiDiffImpl(srcTree, leftV);
        ITree dstTree = scanner.getTree(right);
        DiffImpl diff = mdiff.compute(dstTree, rightV);

        AbstractVersionedTree middle = mdiff.getMiddle();

        // List<ImmutablePair<Integer, AbstractVersionedTree>> l =
        // Combination.flattenItreeToList2(middle,
        // new HashSet<>((List) diff.getActionsList()));
        // int[] init = Combination.initialState(l);
        // int[] leafs = Combination.detectLeafs(l);
        // int[] deps = new int[l.size()];
        // List<Integer> initLeafs = new ArrayList<>();
        // for (int i = 0; i < l.size(); i++) {
        // deps[i] = l.get(i).left;
        // if (leafs[i] == 1) {
        // initLeafs.add(init[i]);
        // }
        // }

        // for (int j=0; j<deps.length;j++) {
        // System.out.print("" + j + " ");
        // }
        // System.out.println();

        // for (int j : deps) {
        // System.out.print("" + (j==-1?" ":j) + " ");
        // }
        // System.out.println();
        List<MyAction<?>> myactions = new ArrayList<>(diff.getActions());

        // TODO add actions

        Flattener.ComposingClusterizer flat = Combination.flatten(new Flattener.Clusterizer(new LinkedHashSet<>(diff.getAtomic())), myactions);
        Set<Cluster> toBreak = new HashSet<>();
        LinkedList<Cluster> tryToBreak = new LinkedList<>();
        flat.setInibiteds(toBreak);
        List<ImmutablePair<Integer, Cluster>> constrainedTree = flat.getConstrainedTree();
        int j = 0;
        int prevSize = 0;
        for (int x : Combination.detectLeafs(constrainedTree)) {
            prevSize += x == 0 ? 0 : 1;
        }
        for (MyAction<?> a : diff.getComposed()) {
            if (a instanceof ComposedAction) {
                tryToBreak.addAll(flat.getCluster((ComposedAction<AbstractVersionedTree>) a));
            }
        }
        while (j < 10) {
            Set<Cluster> tmp = new HashSet<>();
            tmp.addAll(toBreak);
            if (tryToBreak.isEmpty()) {
                break;
            }
            tmp.add(tryToBreak.poll());
            flat.setInibiteds(tmp);
            List<ImmutablePair<Integer, Cluster>> tmp2 = flat.getConstrainedTree();
            int c = 0;
            for (int x : Combination.detectLeafs(constrainedTree)) {
                c += x == 0 ? 0 : 1;
            }
            if (c <= 7 && c>prevSize) {
                prevSize = c;
                toBreak = tmp;
                constrainedTree = tmp2;
            }
            j++;
        }
        ReflectedConstrainedHelper<Cluster> combs = Combination.build(flat,constrainedTree);
        Combination.CHANGE<Integer> next;
        int[] curr = Arrays.copyOf(combs.originalInit, combs.originalInit.length);
        for (int i = 0; i < curr.length; i++) {
            curr[i] = curr[i] > 0 ? 1 : 0;
        }
        int[] ori = Arrays.copyOf(curr, curr.length);
        Map<List<Integer>, Integer> counts = new HashMap<>();
        counts.computeIfAbsent(Arrays.stream(curr).boxed().collect(Collectors.toList()), x -> 0);
        counts.put(Arrays.stream(curr).boxed().collect(Collectors.toList()),
                counts.get(Arrays.stream(curr).boxed().collect(Collectors.toList())) + 1);
        for (int i = 0; i < curr.length; i++) {
            System.out.print("" + i + " ");
        }
        System.out.println();
        for (int k : combs.getLeafs()) {
            System.out.print("" + k + " ");
        }
        System.out.println();
        for (int k : combs.getDeps()) {
            System.out.print("" + (k == -1 ? " " : k) + " ");
        }
        System.out.println();
        for (int k : curr) {
            System.out.print("" + k + " ");
        }
        System.out.println();
        int count = 0;
        do {
            next = combs.nextIndex();
            curr[next.content] = next.way ? 1 : 0;
            List<Integer> tmp = Arrays.stream(curr).boxed().collect(Collectors.toList());
            counts.computeIfAbsent(tmp, x -> 0);
            counts.put(tmp, counts.get(tmp) + 1);
            for (int k : curr) {
                System.out.print("" + k + " ");
            }
            System.out.println("     " + next.content + " " + next.way + " " + counts.get(tmp));
            count++;
        } while (!combs.isInit());
        for (int k : ori) {
            System.out.print("" + k + " ");
        }
        System.out.println();
        for (int k : combs.prevKeyPoint) {
            System.out.print("" + k + " ");
        }
        System.out.println();
        for (int k : combs.nextKeyPoint) {
            System.out.print("" + k + " ");
        }
        System.out.println();
        System.out.println(count);
    }

    /**
     * TODO improvements
     * - merge similar node in middle tree (instead of creating new tree each time),
     *   it needs that added and remove version of merged node should also be merged,
     *   it would allow to improve the combinatorial exploration by reducing the number of states the tree can be in,
     *   it is only needed if there is more than 2 tree versions compared
     */
}