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
    //     this((ITree)other, version);
    //     // Iterator<Entry<String, Object>> map = other.getMetadata();
    //     // while (map.hasNext()) {
    //     //     Entry<String, Object> curr = map.next();
    //     //     this.metadata.set(curr.getKey(), curr.getValue());

    //     // }
    // }

    private VersionedTree(ITree other, int version) {
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
        this.addedVersion = new Version(version);
        this.children = new LinkedList<>();
        this.metadata = new AssociationMap();
    }

    public VersionedTree(ITree other, int version, String... wantedMD) {
        this(other, version);
        for (String key : wantedMD) {
            this.metadata.set(key, other.getMetadata(key));
        }
    }

    @Override
    public VersionedTree deepCopy() {
        VersionedTree copy = new VersionedTree(this, 0);
        for (ITree child : getChildren()) {
            ITree tmp = child.deepCopy();
            copy.addChild(tmp);
            tmp.setParent(copy);
        }
        return copy;
    }

    public static VersionedTree deepCopy(ITree other) {
        VersionedTree copy = new VersionedTree(other, 0);
        for (ITree child : other.getChildren()) {
            ITree tmp = deepCopy(child);
            copy.addChild(tmp);
            tmp.setParent(copy);
        }
        return copy;
    }

    public static VersionedTree deepCopy(ITree other, int version) {
        VersionedTree copy = new VersionedTree(other, version);
        for (ITree child : other.getChildren()) {
            ITree tmp = deepCopy(child);
            copy.addChild(tmp);
            tmp.setParent(copy);
        }
        return copy;
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