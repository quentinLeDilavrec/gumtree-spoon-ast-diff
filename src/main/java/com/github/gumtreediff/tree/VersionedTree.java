package com.github.gumtreediff.tree;

import java.util.ArrayList;
import java.util.HashMap;
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
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;

public class VersionedTree extends AbstractVersionedTree {

    private String label;

    // Begin position of the tree in terms of absolute character index and length
    private int pos;
    private int length;

    private AssociationMap metadata;

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
        this.insertVersion = version;
        this.children = new LinkedList<>();
        this.metadata = new AssociationMap();
        this.metadata.set("type", other.getMetadata("type"));
    }

    private VersionedTree(ITree other, String... wantedMD) {
        this(other, (Version) null);
        for (String key : wantedMD) {
            this.metadata.set(key, other.getMetadata(key));
        }
    }

    public VersionedTree(ITree other, Version version, String... wantedMD) {
        this(other, version);
        if (version == null) {
            throw new UnsupportedOperationException("version must be non null");
        }
        for (String key : wantedMD) {
            this.metadata.set(key, other.getMetadata(key));
        }
    }

    @Override
    public VersionedTree deepCopy() {
        VersionedTree copy = new VersionedTree(this);
        copy.insertVersion = this.insertVersion;
        copy.removeVersion = this.removeVersion;
        for (ITree child : getChildren()) {
            ITree tmp = child.deepCopy();
            copy.addChild(tmp);
            tmp.setParent(copy);
        }
        return copy;
    }

    public static final String ORIGINAL_SPOON_OBJECT = "original_spoon_object";
    // public static final String MIDDLE_GUMTREE_NODE = "middle_gumtree_node";

    public static class AVTfromITreeAlongSpoon {
        Map<ITree, AbstractVersionedTree> oriToClone = new HashMap<>();

        public Map<ITree, AbstractVersionedTree> getMappingFromOri() {
            return oriToClone;
        }

        private MyCloner cloner = null;

        public MyCloner getCloner() {
            return cloner;
        }

        public final AbstractVersionedTree cloned;

        public AVTfromITreeAlongSpoon(ITree ori) {
            cloned = unpopulatedDC(ori);
            cloned.setMetadata("Cloner", cloner);
            cloned.setMetadata("Launcher", cloner.getLauncher());
            cloned.setMetadata("Factory", cloner.getLauncher().getFactory());
        }

        private AbstractVersionedTree unpopulatedDC(ITree currentOrig) {
            CtElement ele = (CtElement) currentOrig.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            AbstractVersionedTree result;
            if (ele != null) {
                result = populatedDC(currentOrig);
                if (cloner == null) {
                    cloner = new MyCloner(ele.getFactory());
                }
                cloner.clone(ele);
            } else {
                result = new VersionedTree(currentOrig);
                for (ITree child : currentOrig.getChildren()) {
                    AbstractVersionedTree copy = unpopulatedDC(child);
                    result.addChild(copy);
                    copy.setParent(result);
                }
            }
            return result;
        }

        private AbstractVersionedTree populatedDC(ITree original) {
            VersionedTree cloned = new VersionedTree(original);
            CtElement ele = (CtElement) original.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            if (ele != null) {
                oriToClone.put(original, cloned);
                // ele.putMetadata(MIDDLE_GUMTREE_NODE, cloned); // might cause memory leak
            }
            cloned.setMetadata(ORIGINAL_SPOON_OBJECT, ele);
            for (ITree child : original.getChildren()) {
                ITree clonedChild = populatedDC(child);
                cloned.addChild(clonedChild);
                clonedChild.setParent(cloned);
            }
            return cloned;
        }

        public class MyCloner extends CloneHelper {
            public final Launcher launcher;

            @Override
            public <T extends CtElement> T clone(T element) {
                ITree clonedAVT = null;
                if (element != null) {
                    ITree ori = (ITree)element.getMetadata(SpoonGumTreeBuilder.GUMTREE_NODE);
                    clonedAVT = oriToClone.get(ori);
                    //(ITree) element.getMetadata(MIDDLE_GUMTREE_NODE); // might cause memory leak
                }
                final CloneVisitorNewFactory cloneVisitor = new CloneVisitorNewFactory(this, launcher.getFactory());
                cloneVisitor.scan(element);
                T clone = cloneVisitor.getClone();
                if (clonedAVT != null) {
                    clonedAVT.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, clone);
                }
                return clone;
            }

            public SourcePosition clone(SourcePosition position) {
                final CloneVisitorNewFactory cloneVisitor = new CloneVisitorNewFactory(this, launcher.getFactory());
                return cloneVisitor.clonePosition(position);
            }

            public MyCloner(Factory pFactory) {
                this.launcher = new Launcher();
            }

            public Launcher getLauncher() {
                return launcher;
            }

        }
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