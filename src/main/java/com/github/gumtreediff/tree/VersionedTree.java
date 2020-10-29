package com.github.gumtreediff.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import com.github.gumtreediff.tree.AbstractTree;
import com.github.gumtreediff.tree.AssociationMap;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import gumtree.spoon.CloneVisitorNewFactory;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.Launcher;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;

public class VersionedTree extends AbstractVersionedTree {

    private String label;

    // Begin position of the tree in terms of absolute character index and length
    private int pos;
    private int length;
    // Start position = pos
    // End position = pos + length

    private AssociationMap metadata;

    public VersionedTree(int type, String label) {
        this.type = type;
        this.label = (label == null) ? NO_LABEL : label.intern();
        this.id = NO_ID;
        this.depth = NO_VALUE;
        this.hash = NO_VALUE;
        this.height = NO_VALUE;
        this.depth = NO_VALUE;
        this.size = NO_VALUE;
        this.pos = NO_VALUE;
        this.length = NO_VALUE;
        this.children = new LinkedList<>();
    }

    // private VersionedTree(AbstractVersionedTree other, int version) {
    // this((ITree)other, version);
    // // Iterator<Entry<String, Object>> map = other.getMetadata();
    // // while (map.hasNext()) {
    // // Entry<String, Object> curr = map.next();
    // // this.metadata.set(curr.getKey(), curr.getValue());

    // // }
    // }

    private VersionedTree(ITree other, Version version) {
        this.type = other.getType();
        this.label = other.getLabel();
        this.id = other.getId();
        this.pos = other.getPos();
        this.length = other.getLength();
        this.height = other.getHeight();
        this.size = other.getSize();
        this.depth = other.getDepth();
        this.hash = other.getHash();
        this.depth = other.getDepth();
        this.depth = other.getDepth();
        this.addedVersion = version;
        this.children = new LinkedList<>();
        this.metadata = new AssociationMap();
        this.metadata.set("type", other.getMetadata("type"));
    }

    public VersionedTree(ITree other, Version version, String... wantedMD) {
        this(other, version);
        for (String key : wantedMD) {
            this.metadata.set(key, other.getMetadata(key));
        }
    }

    @Override
    public VersionedTree deepCopy() {
        VersionedTree copy = new VersionedTree(this, this.getAddedVersion());
        for (ITree child : getChildren()) {
            ITree tmp = child.deepCopy();
            copy.addChild(tmp);
            tmp.setParent(copy);
        }
        return copy;
    }

    public static VersionedTree deepCopy(ITree other, Version version) {
        VersionedTree copy = new VersionedTree(other, version);
        for (ITree child : other.getChildren()) {
            ITree tmp = deepCopy(child, version);
            copy.addChild(tmp);
            tmp.setParent(copy);
        }
        return copy;
    }

    public static VersionedTree deepCopy(ITree other, Version version, String... wantedMD) {
        VersionedTree copy = new VersionedTree(other, version, wantedMD);
        for (ITree child : other.getChildren()) {
            ITree tmp = deepCopy(child, version, wantedMD);
            copy.addChild(tmp);
            tmp.setParent(copy);
        }
        return copy;
    }

    public static final String ORIGINAL_SPOON_OBJECT = "original_spoon_object";
    // public static final String COPIED_SPOON_OBJECT = "copied_spoon_object";
    public static final String MIDDLE_GUMTREE_NODE = "middle_gumtree_node";

    static class MyCloner extends CloneHelper {
        private Launcher launcher;
        
        @Override
        public <T extends CtElement> T clone(T element) {
            ITree gtnode = null;
            if (element != null) {
                gtnode = (ITree) element.getMetadata(MIDDLE_GUMTREE_NODE);
            }
            final CloneVisitorNewFactory cloneVisitor = new CloneVisitorNewFactory(this, launcher.getFactory());
            cloneVisitor.scan(element);
            T clone = cloneVisitor.getClone();
            // if (element instanceof CtRootPackage) {
            //     clone = (T) ((CtPackage) clone).getParent();
            // }
            if (gtnode != null) {
                gtnode.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, clone);
            }
            return clone;
        }

        /**
         * Is called by {@link CloneVisitor} at the end of the cloning for each element.
         */
        public void tailor(final spoon.reflect.declaration.CtElement topLevelElement,
                final spoon.reflect.declaration.CtElement topLevelClone) {
            // this scanner visit certain nodes to done some additional work after cloning
            new CtScanner() {

                @Override
                public <T> void visitCtExecutableReference(CtExecutableReference<T> clone) {
                    // for instance, here we can do additional things
                    // after cloning an executable reference
                    // we have access here to "topLevelElement" and "topLevelClone"
                    // if we want to analyze them as well.

                    // super must be called to visit the subelements
                    super.visitCtExecutableReference(clone);
                }
            }.scan(topLevelClone);
        }

        public MyCloner(Factory pFactory) {
            this.launcher = new Launcher();
        }

        public Launcher getLauncher() {
            return launcher;
        }
    }

    public static AbstractVersionedTree deepCopySpoon(ITree initialSpooned, Version version) {
        ITree currentOrig = initialSpooned;
        CtElement ele;
        AbstractVersionedTree result;
        MyCloner cloner = null;
        do {
            ele = (CtElement) currentOrig.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            if (ele != null) {
                result = deepCopySpoonAux(currentOrig, version);
                cloner = new MyCloner(ele.getFactory());
                cloner.clone(ele);
                result.setMetadata("Launcher", cloner.getLauncher());
                result.setMetadata("Factory", cloner.getLauncher().getFactory());
            } else {
                result = new VersionedTree(currentOrig, version);
                for (ITree child : currentOrig.getChildren()) {
                    AbstractVersionedTree copy = deepCopySpoon(child, version);
                    result.addChild(copy);
                    copy.setParent(result);
                    result.setMetadata("Launcher", copy.getMetadata("Launcher"));
                    result.setMetadata("Factory", copy.getMetadata("Factory"));
                }
                break;
            }
        } while (ele == null);
        if (result.getMetadata("Factory") == null) {
            result.setMetadata("Launcher", new Launcher());
            result.setMetadata("Factory", new Launcher().getFactory());
        }
        return result;
    }

    private static AbstractVersionedTree deepCopySpoonAux(ITree original, Version version) {
        VersionedTree result = new VersionedTree(original, version);
        CtElement ele = (CtElement) original.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        if (ele != null) {
            ele.putMetadata(MIDDLE_GUMTREE_NODE, result);
        }
        result.setMetadata(ORIGINAL_SPOON_OBJECT, ele);
        // copy.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT,
        // original.getMetadata(COPIED_SPOON_OBJECT));
        for (ITree child : original.getChildren()) {
            ITree copy = deepCopySpoonAux(child, version);
            result.addChild(copy);
            copy.setParent(result);
        }
        return result;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public Object getMetadata(String key) {
        if (metadata == null)
            return null;
        return metadata.get(key);
    }

    @Override
    public Object setMetadata(String key, Object value) {
        if (value == null) {
            if (metadata == null)
                return null;
            else
                return metadata.remove(key);
        }
        if (metadata == null)
            metadata = new AssociationMap();
        return metadata.set(key, value);
    }

    @Override
    public Iterator<Entry<String, Object>> getMetadata() {
        if (metadata == null)
            return new EmptyEntryIterator();
        return metadata.iterator();
    }

    protected static class EmptyEntryIterator implements Iterator<Map.Entry<String, Object>> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Map.Entry<String, Object> next() {
            throw new NoSuchElementException();
        }
    }
}