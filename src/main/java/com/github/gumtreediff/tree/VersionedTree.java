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
    public static final String MIDDLE_GUMTREE_NODE = "middle_gumtree_node";

    public static class MyCloner extends CloneHelper {
        public final Launcher launcher;

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

    public static AbstractVersionedTree deepCopySpoon(ITree initialSpooned) {
        ITree currentOrig = initialSpooned;
        CtElement ele;
        AbstractVersionedTree result;
        MyCloner cloner = null;
        do {
            ele = (CtElement) currentOrig.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
            if (ele != null) {
                result = deepCopySpoonAux(currentOrig);
                cloner = new MyCloner(ele.getFactory());
                cloner.clone(ele);
                result.setMetadata("Cloner", cloner);
                result.setMetadata("Launcher", cloner.getLauncher());
                result.setMetadata("Factory", cloner.getLauncher().getFactory());
            } else {
                result = new VersionedTree(currentOrig);
                for (ITree child : currentOrig.getChildren()) {
                    AbstractVersionedTree copy = deepCopySpoon(child);
                    result.addChild(copy);
                    copy.setParent(result);
                    result.setMetadata("Cloner", cloner);
                    result.setMetadata("Launcher", copy.getMetadata("Launcher"));
                    result.setMetadata("Factory", copy.getMetadata("Factory"));
                }
                break;
            }
        } while (ele == null);
        if (result.getMetadata("Factory") == null) {
            result.setMetadata("Cloner", cloner);
            result.setMetadata("Launcher", new Launcher());
            result.setMetadata("Factory", new Launcher().getFactory());
        }
        return result;
    }

    private static AbstractVersionedTree deepCopySpoonAux(ITree original) {
        VersionedTree result = new VersionedTree(original);
        CtElement ele = (CtElement) original.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        if (ele != null) {
            ele.putMetadata(MIDDLE_GUMTREE_NODE, result);
        }
        result.setMetadata(ORIGINAL_SPOON_OBJECT, ele);
        // copy.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT,
        // original.getMetadata(COPIED_SPOON_OBJECT));
        for (ITree child : original.getChildren()) {
            ITree copy = deepCopySpoonAux(child);
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