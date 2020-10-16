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
import com.github.gumtreediff.tree.VersionedTree;

import gnu.trove.map.TIntObjectMap;
import gumtree.spoon.apply.AAction;
import gumtree.spoon.apply.MyUtils;

import java.lang.reflect.Field;
import java.util.*;

public class MyScriptGenerator implements EditScriptGenerator {
    AbstractVersionedTree middle = null;
    MultiVersionMappingStore multiVersionMappingStore;

    public MyScriptGenerator(AbstractVersionedTree middle, MultiVersionMappingStore multiVersionMappingStore) {
        this.middle = middle;
        this.multiVersionMappingStore = multiVersionMappingStore;
    }

    @Override
    public EditScript computeActions(Matcher ms) {
        this.version = (int) middle.getMetadata("lastVersion");
        initWith((SingleVersionMappingStore<AbstractVersionedTree, ITree>) ms.getMappings());
        generate();
        middle.setParent(null);
        origDst.setParent(null);
        return actions;
    }

    int version;

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
        relateMiddleAndSource2();

        cpyMappings = new SingleVersionMappingStore<AbstractVersionedTree, ITree>(middle, origDst);
        for (Mapping m : origMappings)
            cpyMappings.link(origToCopy.get(m.first), m.second);
    }

    private void relateMiddleAndSource() {
        Iterator<AbstractVersionedTree> cpyTreeIterator = (Iterator) TreeUtils.preOrderIterator(middle);
        for (ITree origTree : TreeUtils.preOrder(origSrc)) {
            AbstractVersionedTree cpyTree = cpyTreeIterator.next();
            origToCopy.put(origTree, cpyTree);
            copyToOrig.put(cpyTree, origTree);
        }
    }

    private void relateMiddleAndSource2() {
        aux(middle, origSrc);
    }

    private void aux(AbstractVersionedTree cpyTree, ITree origTree) {
        origToCopy.put(origTree, cpyTree);
        copyToOrig.put(cpyTree, origTree);
        List<AbstractVersionedTree> cpyChildren = (List) cpyTree.getChildren();
        List<ITree> origChildren = origTree.getChildren();
        if (cpyChildren.size() != origChildren.size()) {
            throw new RuntimeException("not same # children");
        }
        for (int i = 0; i < cpyChildren.size(); i++) {
            aux(cpyChildren.get(i), origChildren.get(i));
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
                int k = y.getChildPosition(x);//findPos(x);
                // Insertion case : insert new node.
                w = new VersionedTree(x, this.version); // VersionedTree.deepCopy(x, this.version);//new VersionedTree(x, this.version);
                // In order to use the real nodes from the second tree, we
                // furnish x instead of w
                copyToOrig.put(w, x);
                cpyMappings.link(w, x);
                z.insertChild(w, k);
                w.setParent(z);
                Action action = AAction.build(Insert.class, x, w);//new AInsert(x, w);//new Insert(w, z, k);
                actions.add(action);
                addInsertAction(action, w);
            } else {
                w = cpyMappings.getSrc(x);
                if (!x.equals(origDst)) { // TODO => x != origDst // Case of the root
                    AbstractVersionedTree v = w.getParent();
                    if (!w.getLabel().equals(x.getLabel()) && !z.equals(v)) {
                        AbstractVersionedTree wbis = new VersionedTree(w, this.version);
                        cpyMappings.link(wbis, x);
                        added.add(wbis);
                        boolean qwed = deleted.add(w);
                        int k = y.getChildPosition(x);
                        w.delete(this.version);
                        copyToOrig.put(w, x);
                        ITree gew = copyToOrig.put(wbis, x);
                        z.insertChild(wbis, k);
                        wbis.setLabel(x.getLabel());
                        wbis.setParent(z);
                        multiVersionMappingStore.link(w, wbis);
                        // Action uact = AAction.build(Update.class, w, wbis);
                        // addDeleteAction(uact, w);
                        // addInsertAction(uact, wbis);
                        wbis.setMetadata("alsoUpdated", true);
                        if (NOMOVE) {
                            Action iact = AAction.build(Insert.class, w, wbis);
                            actions.add(iact);
                            // actions.add(uact);
                            Delete dact = AAction.build(Delete.class, w, null);
                            actions.add(dact);
                            ((AbstractVersionedTree) w).delete(version);
                            addDeleteAction(dact, w);
                            addInsertAction(iact, wbis);
                        } else {
                            Action mact = AAction.build(Move.class, w, wbis);
                            actions.add(mact);
                            // actions.add(uact);
                            addDeleteAction(mact, w);
                            addInsertAction(mact, wbis);
                        }
                    } else if (!w.getLabel().equals(x.getLabel())) {
                        AbstractVersionedTree wbis = new VersionedTree(w, this.version);//VersionedTree.deepCopy(w, this.version);
                        cpyMappings.link(wbis, x);
                        added.add(wbis);
                        boolean qwed = deleted.add(w);
                        int k = v.getChildPosition(w);
                        //cpyMappings.unlink(w, x);
                        //cpyMappings.link(wbis, x);
                        w.delete(this.version);
                        copyToOrig.put(w, x);
                        ITree gew = copyToOrig.put(wbis, x);
                        // copyToOrig.put(w, x);
                        // cpyMappings.link(w, x);
                        v.insertChild(wbis, k);
                        wbis.setLabel(x.getLabel());
                        wbis.setParent(v);
                        multiVersionMappingStore.link(w, wbis);
                        Action action = AAction.build(Update.class, w, wbis);//new AUpdate(w, wbis);
                        actions.add(action); // TODO put removedVersion and added version ?
                        addDeleteAction(action, w);
                        addInsertAction(action, wbis);
                    } else if (!z.equals(v)) {
                        int k = y.getChildPosition(x);//findPos(x);
                        // int oldk = w.positionInParent();
                        AbstractVersionedTree wbis = new VersionedTree(w, this.version);// VersionedTree.deepCopy(w, this.version);
                        wbis.setParent(z);
                        z.insertChild(wbis, k);
                        cpyMappings.link(wbis, x);
                        added.add(wbis);
                        boolean qwed = deleted.add(w);
                        //cpyMappings.unlink(x, w);
                        w.delete(this.version);
                        copyToOrig.put(w, x);
                        ITree gew = copyToOrig.put(wbis, x);
                        // cpyMappings.link(w, x);
                        multiVersionMappingStore.link(w, wbis);
                        if (NOMOVE) {
                            Action iact = AAction.build(Insert.class, w, wbis);//new AMove(w, wbis); // TODO put removedVersion and added version ?
                            actions.add(iact);
                            Delete dact = AAction.build(Delete.class, w, null);//new ADelete(w);
                            actions.add(dact);
                            ((AbstractVersionedTree) w).delete(version);
                            addDeleteAction(dact, w);
                            addInsertAction(iact, wbis);
                        } else {
                            Action action = AAction.build(Move.class, w, wbis);//new AMove(w, wbis); // TODO put removedVersion and added version ?
                            actions.add(action);
                            addDeleteAction(action, w);
                            addInsertAction(action, wbis);
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

    static boolean NOMOVE = true;

    private void handleDeletion() {
        List<ITree> preOMiddle = TreeUtils.preOrder(middle);
        List<ITree> pOMiddle = TreeUtils.postOrder(middle);
        for (ITree w : pOMiddle)//middle.postOrder())
            if (!cpyMappings.hasSrc(w)) {
                Delete action = AAction.build(Delete.class, w, null);//new ADelete(w);
                actions.add(action); // TODO cannot find all nodes, related to hash ?
                ((AbstractVersionedTree) w).delete(version);
                addDeleteAction(action, w);
            }
    }

    private void handleDeletion2() {
        handleDeletion2Aux(middle);
    }

    private void handleDeletion2Aux(AbstractVersionedTree w) {
        List<AbstractVersionedTree> children = w.getChildren(this.version - 1);
        for (int i = children.size() - 1; i >= 0; i--) {
            handleDeletion2Aux(children.get(i));
        }
        if (!cpyMappings.hasSrc(w)) {
            if (w instanceof AbstractVersionedTree && ((AbstractVersionedTree) w).getAddedVersion() == this.version) {
                System.err.println(w);
            } else {
                Action action = AAction.build(Delete.class, w, null);//new ADelete(w);
                actions.add(action); // TODO cannot find all nodes, related to hash ?
                ((AbstractVersionedTree) w).delete(version);
                addDeleteAction(action, w);
            }
        }
    }

    private static boolean isDoingDelete(Action action) {
        if (action instanceof Delete) {
            return true;
        } else if (action instanceof Move) {
            return true;
        } else if (action instanceof Update) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isDoingInsert(Action action) {
        if (action instanceof Insert) {
            return true;
        } else if (action instanceof Move) {
            return true;
        } else if (action instanceof Update) {
            return true;
        } else {
            return false;
        }
    }

    private void addAction(Action action, ITree w) {
        List<Action> tmp = (List) w.getMetadata("action");
        if (tmp == null) {
            tmp = new ArrayList<>();
            w.setMetadata("action", tmp);
        }
        tmp.add(action);
    }

    public static String DELETE_ACTION = "DELETE_ACTION";
    public static String INSERT_ACTION = "INSERT_ACTION";

    private Action addDeleteAction(Action action, ITree w) {
        return (Action) w.setMetadata(DELETE_ACTION, action);
    }

    private Action addInsertAction(Action action, ITree w) {
        return (Action) w.setMetadata(INSERT_ACTION, action);
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
                        AbstractVersionedTree abis = new VersionedTree(a, this.version);
                        abis.setParent(w);
                        w.insertChild(abis, k);
                        cpyMappings.link(abis, b);
                        ((AbstractVersionedTree)a).delete(this.version);
                        copyToOrig.put((AbstractVersionedTree)a, x);
                        copyToOrig.put(abis, x);
                        multiVersionMappingStore.link(a, abis);
                        if (NOMOVE) {
                            Action iact = AAction.build(Insert.class, a, abis);
                            actions.add(iact);
                            Delete dact = AAction.build(Delete.class, a, null);
                            actions.add(dact);
                            ((AbstractVersionedTree) a).delete(version);
                            addDeleteAction(dact, a);
                            addInsertAction(iact, abis);
                        } else {
                            Action action = AAction.build(Move.class, a, abis);
                            actions.add(action);
                            addDeleteAction(action, a);
                            addInsertAction(action, abis);
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

    private List<Action> removeMovesAndUpdates(MyScriptGenerator actionGenerator) {
        try {
            TIntObjectMap<ITree> origSrcTrees = extracted(actionGenerator, "origSrcTrees");
            TIntObjectMap<ITree> cpySrcTrees = extracted(actionGenerator, "cpySrcTrees");
            List<Action> actions = extracted(actionGenerator, "actions");
            MappingStore origMappings = extracted(actionGenerator, "origMappings");
            List<Action> actionsCpy = new ArrayList<>(actions.size());
            for (Action a : actions) {
                if (a instanceof Update) {
                    Update u = (Update) a;
                    ITree src = cpySrcTrees.get(a.getNode().getId());
                    ITree dst = origMappings.getDst(src);
                    actionsCpy.add(new Insert(dst, dst.getParent(), dst.positionInParent()));
                    actionsCpy.add(new Delete(origSrcTrees.get(u.getNode().getId())));
                } else if (a instanceof Move) {
                    Move m = (Move) a;
                    ITree src = cpySrcTrees.get(a.getNode().getId());
                    ITree dst = origMappings.getDst(src);
                    actionsCpy.add(new Insert(dst, dst.getParent(), m.getPosition()));
                    actionsCpy.add(new Delete(origSrcTrees.get(m.getNode().getId())));
                } else {
                    actionsCpy.add(a);
                }
            }

            return actionsCpy;
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T extracted(MyScriptGenerator actionGenerator, String name)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = actionGenerator.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(actionGenerator);
    }

}