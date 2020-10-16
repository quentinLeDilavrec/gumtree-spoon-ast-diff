/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree;

import com.github.gumtreediff.tree.hash.HashUtils;

import java.util.*;

public abstract class AbstractVersionedTree implements ITree {

    // TODO use ℚ because it's dense, it will avoid recounting all versions if we want to add a new version inbetween existing versions
    public static class Version implements Comparable<Version> {
        int value;

        Version(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(Version o) {
            return value - o.value;
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }

    protected int id;
    // the version where the node was added to the tree
    protected Version addedVersion; // cannot be null
    protected Version removedVersion; // can be null, i.e not removed

    protected AbstractVersionedTree parent;

    protected LinkedList<AbstractVersionedTree> children;

    protected int type;

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    public int getAddedVersion() {
        return addedVersion.value;
    }

    public int getRemovedVersion() {
        return removedVersion.value;
    }

    public boolean isRemoved() {
        return removedVersion!=null;
    }

    // /**
    //  * Doubly LinkedList
    //  */
    // static class LL<T> {

    //     Node head; // head of list
    //     Node tail; // tail of list

    //     Node node(T d){ return new Node(d);}

    //     class Node {
    //         T content;
    //         Node prev;
    //         Node next;

    //         Node(T d) {
    //             content = d;
    //         }
    //     }

    // }

    protected int height;

    protected int size;

    protected int depth;

    protected int hash;

    @Override
    public int getChildPosition(ITree child) {
        if (!(child instanceof AbstractVersionedTree))
            return -1;
        int i = 0;
        Version childVersion = ((AbstractVersionedTree) child).addedVersion;
        for (AbstractVersionedTree curr : children) {
            if (curr==child) {
                return i;
            } else if (childVersion.compareTo(curr.addedVersion) >= 0) {
                ++i;
            }
        }
        return -1;
    }

    public int getChildPosition(AbstractVersionedTree child, Version maxVersion) {
        int i = 0;
        for (AbstractVersionedTree curr : children) {
            if (curr==child) {
                return i;
            } else if (maxVersion.compareTo(curr.addedVersion) >= 0) {
                ++i;
            }
        }
        return -1;
    }

    @Override
    public ITree getChild(int position) {
        return getChildren().get(position);
    }

    @Override
    public List<ITree> getChildren() {
        List<ITree> r = new ArrayList<>();
        for (AbstractVersionedTree curr : children) {
            if (curr.removedVersion == null) {
                r.add(curr);
            }
        }
        return r;
    }

    public List<AbstractVersionedTree> getChildren(int wantedVersion) {
        return getChildren(new Version(wantedVersion));
    }

    public List<AbstractVersionedTree> getChildren(Version wantedVersion) {
        List<AbstractVersionedTree> r = new ArrayList<>();
        for (AbstractVersionedTree curr : children) {
            if (curr.removedVersion == null) {
                if (wantedVersion.compareTo(curr.addedVersion) >= 0) {
                    r.add(curr);
                }
            } else if (wantedVersion.compareTo(curr.addedVersion) >= 0
                    && wantedVersion.compareTo(curr.removedVersion) < 0) {
                r.add(curr);
            }
        }
        return r;
    }

    public List<AbstractVersionedTree> getAllChildren() {
        List<AbstractVersionedTree> r = new ArrayList<>();
        for (AbstractVersionedTree curr : children) {
            r.add(curr);
        }
        return r;
    }

    public AbstractVersionedTree getChild(Version wantedVersion, int position) {
        return getChildren(wantedVersion).get(position);
    }

    public AbstractVersionedTree getChild(int wantedVersion, int position) {
        return getChild(new Version(wantedVersion), position);
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public List<ITree> getDescendants() {
        List<ITree> trees = TreeUtils.preOrder(this);
        trees.remove(0);
        return trees;
    }

    @Override
    public int getHash() {
        return hash;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean hasLabel() {
        return !NO_LABEL.equals(getLabel());
    }

    @Override
    public AbstractVersionedTree getParent() {
        return parent;
    }

    @Override
    public void setParent(ITree parent) {
        this.parent = (AbstractVersionedTree) parent;
    }

    @Override
    public List<ITree> getParents() {
        List<ITree> parents = new ArrayList<>();
        if (getParent() == null)
            return parents;
        else {
            parents.add(getParent());
            parents.addAll(getParent().getParents());
        }
        return parents;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public List<ITree> getTrees() {
        return TreeUtils.preOrder(this);
    }

    @Override
    public void setChildren(List<ITree> children) {
        this.children = (LinkedList) children;
        for (ITree c : this.children)
            c.setParent(this);
    }

    @Override
    public void setParentAndUpdateChildren(ITree parent) {
        if (this.parent != null)
            this.parent.getChildren().remove(this);
        this.parent = (AbstractVersionedTree) parent;
        if (this.parent != null)
            parent.getChildren().add(this);
    }

    @Override
    public void addChild(ITree child) {
        if (!(child instanceof AbstractVersionedTree))
            throw new RuntimeException("should be an AbstractVersionedTree");
        children.add((AbstractVersionedTree) child);
    }

    // TODO versioned version ?

    @Override
    public void insertChild(ITree child, int position) {
        if (!(child instanceof AbstractVersionedTree))
            throw new RuntimeException("should be an AbstractVersionedTree");
        Version childAddedVersion = ((AbstractVersionedTree) child).addedVersion;
        int j = 0;
        for (int i = 0; i < children.size(); i++) {
            if (j == position) {
                children.add(i, (AbstractVersionedTree) child);
                return;
            } else if (children.get(i).addedVersion.compareTo(childAddedVersion) <= 0
                    && (children.get(i).removedVersion == null
                            || children.get(i).removedVersion.compareTo(childAddedVersion) > 0)) {
                j++;
            }
        }
        children.add(children.size(), (AbstractVersionedTree) child);
    }

    private String indent(ITree t) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.getDepth(); i++)
            b.append("\t");
        return b.toString();
    }

    @Override
    public boolean isIsomorphicTo(ITree tree) {
        if (this.getHash() != tree.getHash())
            return false;
        else
            return this.toStaticHashString().equals(tree.toStaticHashString());
    }

    @Override
    public boolean hasSameType(ITree t) {
        return getType() == t.getType();
    }

    @Override
    public boolean isLeaf() {
        return getChildren().size() == 0;
    }

    // TODO isLeafVersioned

    @Override
    public boolean isRoot() {
        return getParent() == null;
    }

    public void delete(int version) {
        this.removedVersion = new Version(version);
    }

    @Override
    public boolean hasSameTypeAndLabel(ITree t) {
        if (!hasSameType(t))
            return false;
        else if (!getLabel().equals(t.getLabel()))
            return false;
        return true;
    }

    @Override
    public Iterable<ITree> preOrder() {
        return new Iterable<ITree>() {
            @Override
            public Iterator<ITree> iterator() {
                return TreeUtils.preOrderIterator(AbstractVersionedTree.this);
            }
        };
    }

    @Override
    public Iterable<ITree> postOrder() {
        return new Iterable<ITree>() {
            @Override
            public Iterator<ITree> iterator() {
                return TreeUtils.postOrderIterator(AbstractVersionedTree.this);
            }
        };
    }

    @Override
    public Iterable<ITree> breadthFirst() {
        return new Iterable<ITree>() {
            @Override
            public Iterator<ITree> iterator() {
                return TreeUtils.breadthFirstIterator(AbstractVersionedTree.this);
            }
        };
    }

    @Override
    public int positionInParent() {
        AbstractVersionedTree p = (AbstractVersionedTree) getParent();
        if (p == null)
            return -1;
        else
            return p.getChildPosition(this);
    }

    @Override
    public void refresh() {
        TreeUtils.computeSize(this);
        TreeUtils.computeDepth(this);
        TreeUtils.computeHeight(this);
        HashUtils.DEFAULT_HASH_GENERATOR.hash(this);
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public void setHash(int digest) {
        this.hash = digest;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toStaticHashString() {
        StringBuilder b = new StringBuilder();
        b.append(OPEN_SYMBOL);
        b.append(this.toShortString());
        for (ITree c : this.getChildren())
            b.append(c.toStaticHashString());
        b.append(CLOSE_SYMBOL);
        return b.toString();
    }

    @Override
    public String toString() {
        System.err.println("This method should currently not be used (please use toShortString())");
        return toShortString();
    }

    @Override
    public String toShortString() {
        return String.format("%s%s%s", getType(), SEPARATE_SYMBOL, getLabel());
    }

    @Override
    public String toTreeString() {
        StringBuilder b = new StringBuilder();
        for (ITree t : TreeUtils.preOrder(this))
            b.append(indent(t) + ((AbstractVersionedTree) t).addedVersion + " " + t.toShortString() + "\n");
        return b.toString();
    }

    @Override
    public String toPrettyString(TreeContext ctx) {
        if (hasLabel())
            return ctx.getTypeLabel(this) + ": " + getLabel();
        else
            return ctx.getTypeLabel(this);
    }

    public static class FakeTree extends AbstractVersionedTree {
        public FakeTree(ITree... trees) {
            children = new LinkedList<>();
            children.addAll((List) Arrays.asList(trees));
        }

        private RuntimeException unsupportedOperation() {
            return new UnsupportedOperationException("This method should not be called on a fake tree");
        }

        @Override
        public void addChild(ITree t) {
            throw unsupportedOperation();
        }

        @Override
        public void insertChild(ITree t, int position) {
            throw unsupportedOperation();
        }

        @Override
        public ITree deepCopy() {
            throw unsupportedOperation();
        }

        @Override
        public List<ITree> getChildren() {
            return (List) children;
        }

        @Override
        public String getLabel() {
            return NO_LABEL;
        }

        @Override
        public int getLength() {
            return getEndPos() - getPos();
        }

        @Override
        public int getPos() {
            return Collections.min(children, (t1, t2) -> t2.getPos() - t1.getPos()).getPos();
        }

        @Override
        public int getEndPos() {
            return Collections.max(children, (t1, t2) -> t2.getPos() - t1.getPos()).getEndPos();
        }

        @Override
        public int getType() {
            return -1;
        }

        @Override
        public void setChildren(List<ITree> children) {
            throw unsupportedOperation();
        }

        @Override
        public void setLabel(String label) {
            throw unsupportedOperation();
        }

        @Override
        public void setLength(int length) {
            throw unsupportedOperation();
        }

        @Override
        public void setParentAndUpdateChildren(ITree parent) {
            throw unsupportedOperation();
        }

        @Override
        public void setPos(int pos) {
            throw unsupportedOperation();
        }

        @Override
        public void setType(int type) {
            throw unsupportedOperation();
        }

        @Override
        public String toPrettyString(TreeContext ctx) {
            return "FakeTree";
        }

        /**
         * fake nodes have no metadata
         */
        @Override
        public Object getMetadata(String key) {
            return null;
        }

        /**
         * fake node store no metadata
         */
        @Override
        public Object setMetadata(String key, Object value) {
            return null;
        }

        /**
         * Since they have no metadata they do not iterate on nothing
         */
        @Override
        public Iterator<Map.Entry<String, Object>> getMetadata() {
            return new EmptyEntryIterator();
        }
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
