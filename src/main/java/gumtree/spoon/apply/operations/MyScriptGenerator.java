package gumtree.spoon.apply.operations;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.MultiVersionMappingStore;
import com.github.gumtreediff.matchers.SingleVersionMappingStore;
import com.github.gumtreediff.tree.AbstractTree;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionedTree;

import gnu.trove.map.TIntObjectMap;
import gumtree.spoon.apply.AAction;
import gumtree.spoon.apply.MyUtils;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.reflect.declaration.CtElement;

import java.lang.reflect.Field;
import java.util.*;

public class MyScriptGenerator implements EditScriptGenerator {
    AbstractVersionedTree middle = null;
    MultiVersionMappingStore multiVersionMappingStore;

    public enum Granularity {
        ATOMIC, COMPOSE, SPLITED;
    }

    final Granularity granularity;
    Version beforeVersion;
    Version afterVersion;

    public MyScriptGenerator(AbstractVersionedTree middle, MultiVersionMappingStore multiVersionMappingStore,
            Granularity granularity) {
        this.middle = middle;
        this.multiVersionMappingStore = multiVersionMappingStore;
        this.granularity = granularity;
    }

    @Override
    public EditScript computeActions(Matcher ms, Version beforeVersion, Version afterVersion) {
        this.beforeVersion = beforeVersion;
        this.afterVersion = afterVersion;
        initWith((SingleVersionMappingStore<AbstractVersionedTree, ITree>) ms.getMappings());
        generate();
        middle.setParent(null);
        origDst.setParent(null);
        return actions;
    }

    private ITree origSrc;

    private ITree origDst;

    private SingleVersionMappingStore<AbstractVersionedTree, ITree> origMappings;

    private SingleVersionMappingStore<AbstractVersionedTree, ITree> cpyMappings;

    private Set<ITree> dstInOrder;

    private Set<ITree> srcInOrder;

    private EditScript actions;

    private Map<ITree, AbstractVersionedTree> origToCopy;

    private Map<AbstractVersionedTree, ITree> copyToOrig;

    public void initWith(SingleVersionMappingStore<AbstractVersionedTree, ITree> ms) {
        this.origMappings = ms;
        this.origSrc = ms.getSrc();
        this.origDst = ms.getDst();

        origToCopy = new HashMap<>();
        copyToOrig = new HashMap<>();
        relateMiddleAndSource(middle, origSrc);

        cpyMappings = new SingleVersionMappingStore<AbstractVersionedTree, ITree>(middle, origDst);
        for (Mapping m : origMappings) {
            cpyMappings.link(origToCopy.get(m.first), m.second);
            // multiVersionMappingStore.link(origToCopy.get(m.first), m.second);
        }
    }

    private void relateMiddleAndSource(AbstractVersionedTree cpyTree, ITree origTree) {
        origToCopy.put(origTree, cpyTree);
        copyToOrig.put(cpyTree, origTree);
        List<AbstractVersionedTree> cpyChildren = (List) cpyTree.getChildren();
        List<ITree> origChildren = origTree.getChildren();
        if (cpyChildren.size() != origChildren.size()) {
            throw new RuntimeException("not same number of children");
        }
        for (int i = 0; i < cpyChildren.size(); i++) {
            relateMiddleAndSource(cpyChildren.get(i), origChildren.get(i));
        }
    }

    public EditScript generate() {
        AbstractVersionedTree srcFakeRoot = new AbstractVersionedTree.FakeTree(middle);
        ITree dstFakeRoot = new AbstractTree.FakeTree(origDst);
        middle.setParent(srcFakeRoot);
        origDst.setParent(dstFakeRoot);

        actions = new EditScript();
        dstInOrder = new HashSet<>();
        srcInOrder = new HashSet<>();

        cpyMappings.link(srcFakeRoot, dstFakeRoot);
        Set<ITree> deleted = new HashSet<>();
        Set<ITree> added = new HashSet<>();
        List<ITree> bfsDst = TreeUtils.breadthFirst(origDst);
        for (ITree x : bfsDst) {
            AbstractVersionedTree w;
            ITree y = x.getParent();//Tree
            AbstractVersionedTree z = cpyMappings.getSrc(y);

            if (!cpyMappings.hasDst(x)) {
                int k = y.getChildPosition(x);
                // Insertion case : insert new node.
                w = new VersionedTree(x, this.afterVersion);
                mdForMiddle(x, w);
                copyToOrig.put(w, x);
                cpyMappings.link(w, x);
                z.insertChild(w, k);
                w.setParent(z);
                Action action = AAction.build(Insert.class, x, w);
                actions.add(action);
                addInsertAction(action, w);
            } else {
                w = cpyMappings.getSrc(x);
                if (!x.equals(origDst)) { // TODO => x != origDst // Case of the root
                    AbstractVersionedTree v = w.getParent();
                    if (!w.getLabel().equals(x.getLabel()) && !z.equals(v)) {
                        // x was renamed and moved from z to y
                        // in intermediate: w is was moved from v to z,
                        // thus w is marked as deleted and newTree is created
                        AbstractVersionedTree newTree = new VersionedTree(x, this.afterVersion);
                        mdForMiddle(x, newTree);
                        cpyMappings.link(newTree, x);
                        added.add(newTree);
                        deleted.add(w);
                        int k = y.getChildPosition(x);
                        w.delete(this.afterVersion);
                        copyToOrig.put(w, x);
                        copyToOrig.put(newTree, x);
                        z.insertChild(newTree, k);
                        newTree.setLabel(x.getLabel());
                        newTree.setParent(z);
                        multiVersionMappingStore.link(w, newTree);
                        // Action uact = AAction.build(Update.class, w, wbis);
                        // addDeleteAction(uact, w);
                        // addInsertAction(uact, wbis);
                        newTree.setMetadata("alsoUpdated", true);
                        switch (granularity) {
                            case COMPOSE: {
                                Move mact = AAction.build(Move.class, w, newTree);
                                actions.add(mact);
                                // actions.add(uact);
                                addDeleteAction(mact, w);
                                addInsertAction(mact, newTree);
                                break;
                            }
                            case ATOMIC: {
                                Action iact = AAction.build(Insert.class, w, newTree);
                                actions.add(iact);
                                // actions.add(uact);
                                Delete dact = AAction.build(Delete.class, w, null);
                                actions.add(dact);
                                ((AbstractVersionedTree) w).delete(this.afterVersion);
                                addDeleteAction(dact, w);
                                addInsertAction(iact, newTree);
                                break;
                            }
                            case SPLITED: {
                                Action iact = AAction.build(Insert.class, w, newTree);
                                actions.add(iact);
                                // actions.add(uact);
                                Delete dact = AAction.build(Delete.class, w, null);
                                actions.add(dact);
                                ((AbstractVersionedTree) w).delete(this.afterVersion);
                                addDeleteAction(dact, w);
                                addInsertAction(iact, newTree);
                                Move mact = AAction.build(Move.class, w, newTree);
                                actions.addComposed(mact);
                                addMoveAction(mact, x, w, newTree);
                                break;
                            }
                        }
                    } else if (!w.getLabel().equals(x.getLabel())) {
                        // x was renamed
                        // in intermediate: w is marked as deleted,
                        // newTree is created with new label
                        AbstractVersionedTree newTree = new VersionedTree(x, this.afterVersion);
                        mdForMiddle(x, newTree);
                        cpyMappings.link(newTree, x);
                        added.add(newTree);
                        deleted.add(w);
                        int k = v.getChildPosition(w);
                        w.delete(this.afterVersion);
                        copyToOrig.put(w, x);
                        copyToOrig.put(newTree, x);
                        v.insertChild(newTree, k);
                        newTree.setLabel(x.getLabel());
                        newTree.setParent(v);
                        multiVersionMappingStore.link(w, newTree);
                        Action action = AAction.build(Update.class, w, newTree);
                        actions.add(action);
                        addDeleteAction(action, w);
                        addInsertAction(action, newTree);
                    } else if (!z.equals(v)) {
                        // x was moved from z to y
                        // in intermediate: w is was moved from v to z,
                        // thus w is marked as deleted and newTree is created
                        int k = y.getChildPosition(x);
                        AbstractVersionedTree newTree = new VersionedTree(x, this.afterVersion);
                        mdForMiddle(x, newTree);
                        newTree.setParent(z);
                        z.insertChild(newTree, k);
                        cpyMappings.link(newTree, x);
                        added.add(newTree);
                        deleted.add(w);
                        w.delete(this.afterVersion);
                        copyToOrig.put(w, x);
                        copyToOrig.put(newTree, x);
                        multiVersionMappingStore.link(w, newTree);
                        switch (granularity) {
                            case COMPOSE: {
                                Action mact = AAction.build(Move.class, w, newTree);
                                actions.add(mact);
                                addDeleteAction(mact, w);
                                addDeleteAction(mact, x);
                                addInsertAction(mact, newTree);
                                break;
                            }
                            case ATOMIC: {
                                Action iact = AAction.build(Insert.class, w, newTree);
                                actions.add(iact);
                                Delete dact = AAction.build(Delete.class, w, null);
                                actions.add(dact);
                                w.delete(this.afterVersion);
                                addDeleteAction(dact, w);
                                addDeleteAction(dact, x);
                                addInsertAction(iact, newTree);
                                break;
                            }
                            case SPLITED: {
                                Action iact = AAction.build(Insert.class, w, newTree);
                                actions.add(iact);
                                Delete dact = AAction.build(Delete.class, w, null);
                                actions.add(dact);
                                w.delete(this.afterVersion);
                                addDeleteAction(dact, w);
                                addDeleteAction(dact, x);
                                addInsertAction(iact, newTree);
                                Move mact = AAction.build(Move.class, w, newTree);
                                actions.addComposed(mact);
                                addMoveAction(mact, x, w, newTree);
                                break;
                            }
                        }
                    }

                }
            }
            srcInOrder.add(w);
            dstInOrder.add(x);
            alignChildren(w, x);
        }

        handleDeletion2();

        return actions;
    }

    public static String ORIGINAL_SPOON_OBJECT_PER_VERSION = "ORIGINAL_SPOON_OBJECT_PER_VERSION";

    private void mdForMiddle(ITree original, AbstractVersionedTree middle) {
        CtElement ele = (CtElement)original.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        if (ele == null) {
            ele = (CtElement)original.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
        }
        CtElement oldOri = (CtElement)middle.setMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT, ele);
        Map<Version,CtElement> tmp = (Map<Version,CtElement>)middle.getMetadata(ORIGINAL_SPOON_OBJECT_PER_VERSION);
        if (tmp == null) {
            tmp = new HashMap<>();
            middle.setMetadata(ORIGINAL_SPOON_OBJECT_PER_VERSION, tmp);
        }
        tmp.put(this.beforeVersion, oldOri != null ? oldOri : ele);
        tmp.put(this.afterVersion, ele);
        ele.putMetadata(VersionedTree.MIDDLE_GUMTREE_NODE, middle);
    }

    private void handleDeletion2() {
        handleDeletion2Aux(middle);
    }

    private void handleDeletion2Aux(AbstractVersionedTree w) {
        List<AbstractVersionedTree> children = w.getChildren(this.beforeVersion);
        for (int i = children.size() - 1; i >= 0; i--) {
            handleDeletion2Aux(children.get(i));
        }
        if (!cpyMappings.hasSrc(w)) {
            if (w instanceof AbstractVersionedTree && ((AbstractVersionedTree) w).getAddedVersion() == this.afterVersion) {
                System.err.println(w);
            } else {
                Action action = AAction.build(Delete.class, w, null);
                actions.add(action);
                ((AbstractVersionedTree) w).delete(this.afterVersion);
                addDeleteAction(action, w);
            }
        }
    }

    public static String DELETE_ACTION = "DELETE_ACTION";
    public static String INSERT_ACTION = "INSERT_ACTION";

    private Action addDeleteAction(Action action, ITree w) {
        return (Action) w.setMetadata(DELETE_ACTION, action);
    }

    private Action addInsertAction(Action action, ITree w) {
        return (Action) w.setMetadata(INSERT_ACTION, action);
    }

    public static String MOVE_SRC_ACTION = "MOVE_SRC_ACTION";
    public static String MOVE_DST_ACTION = "MOVE_DST_ACTION";

    private void addMoveAction(Move action, ITree x, AbstractVersionedTree w, AbstractVersionedTree wbis) {
        assert x.setMetadata(MOVE_SRC_ACTION, action) == null;
        assert w.setMetadata(MOVE_SRC_ACTION, action) == null;
        assert wbis.setMetadata(MOVE_DST_ACTION, action) == null;
    }

    private void alignChildren(ITree w, ITree x) {
        srcInOrder.removeAll(w.getChildren()); // TODO look at it !
        dstInOrder.removeAll(x.getChildren());

        List<ITree> s1 = new ArrayList<>();
        for (ITree c : w.getChildren())
            if (cpyMappings.hasSrc(c))
                if (x.getChildren().contains(cpyMappings.getDst(c)))
                    s1.add(c);

        List<ITree> s2 = new ArrayList<>();
        for (ITree c : x.getChildren())
            if (cpyMappings.hasDst(c))
                if (w.getChildren().contains(cpyMappings.getSrc(c)))
                    s2.add(c);

        List<Mapping> lcs = lcs(s1, s2);

        for (Mapping m : lcs) {
            srcInOrder.add(m.first);
            dstInOrder.add(m.second);
        }

        for (ITree b : s2) { // iterate through s2 first, to ensure left-to-right insertions
            for (ITree a : s1) {
                if (cpyMappings.has(a, b)) {
                    if (!lcs.contains(new Mapping(a, b))) {
                        int k = x.getChildPosition(b);
                        AbstractVersionedTree newTree = new VersionedTree(b, this.afterVersion);
                        mdForMiddle(b, newTree);
                        newTree.setParent(w);
                        w.insertChild(newTree, k);
                        cpyMappings.link(newTree, b);
                        ((AbstractVersionedTree) a).delete(this.afterVersion);
                        copyToOrig.put((AbstractVersionedTree) a, x);
                        copyToOrig.put(newTree, x);
                        multiVersionMappingStore.link(a, newTree);
                        switch (granularity) {
                            case COMPOSE: {
                                Action action = AAction.build(Move.class, a, newTree);
                                actions.add(action);
                                addDeleteAction(action, a);
                                addInsertAction(action, newTree);
                                break;
                            }
                            case ATOMIC: {
                                Action iact = AAction.build(Insert.class, a, newTree);
                                actions.add(iact);
                                Delete dact = AAction.build(Delete.class, a, null);
                                actions.add(dact);
                                ((AbstractVersionedTree) a).delete(this.afterVersion);
                                addDeleteAction(dact, a);
                                addInsertAction(iact, newTree);
                                break;
                            }
                            case SPLITED: {
                                Action iact = AAction.build(Insert.class, a, newTree);
                                actions.add(iact);
                                Delete dact = AAction.build(Delete.class, a, null);
                                actions.add(dact);
                                ((AbstractVersionedTree) a).delete(this.afterVersion);
                                addDeleteAction(dact, a);
                                addInsertAction(iact, newTree);
                                Move action = AAction.build(Move.class, a, newTree);
                                actions.addComposed(action);
                                addMoveAction(action,b, (AbstractVersionedTree) a, newTree);
                                break;
                            }
                        }
                        srcInOrder.add(a);
                        dstInOrder.add(b);
                    }
                }
            }
        }
    }

    private int findPos(ITree x) {
        ITree y = x.getParent();
        List<ITree> siblings = y.getChildren();

        for (ITree c : siblings) {
            if (dstInOrder.contains(c)) {
                if (c.equals(x))
                    return 0;
                else
                    break;
            }
        }

        int xpos = x.positionInParent();
        ITree v = null;
        for (int i = 0; i < xpos; i++) {
            ITree c = siblings.get(i);
            if (dstInOrder.contains(c))
                v = c;
        }

        //if (v == null) throw new RuntimeException("No rightmost sibling in order");
        if (v == null)
            return 0;

        ITree u = cpyMappings.getSrc(v);
        // siblings = u.getParent().getChildren();
        // int upos = siblings.indexOf(u);
        int upos = u.positionInParent();
        // int r = 0;
        // for (int i = 0; i <= upos; i++)
        // if (srcInOrder.contains(siblings.get(i))) r++;
        return upos + 1;
    }

    private List<Mapping> lcs(List<ITree> x, List<ITree> y) {
        int m = x.size();
        int n = y.size();
        List<Mapping> lcs = new ArrayList<>();

        int[][] opt = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (cpyMappings.getSrc(y.get(j)).equals(x.get(i)))
                    opt[i][j] = opt[i + 1][j + 1] + 1;
                else
                    opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
            }
        }

        int i = 0, j = 0;
        while (i < m && j < n) {
            if (cpyMappings.getSrc(y.get(j)).equals(x.get(i))) {
                lcs.add(new Mapping(x.get(i), y.get(j)));
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1])
                i++;
            else
                j++;
        }

        return lcs;
    }
}