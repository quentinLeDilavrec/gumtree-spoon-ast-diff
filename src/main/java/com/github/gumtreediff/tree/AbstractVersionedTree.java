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

    protected int id;
    // the version where the node was added to the tree
    protected Version insertVersion; // can be null, i.e not knowing when added
    protected Version removeVersion; // can be null, i.e not removed

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

    public Version getInsertVersion() {
        return insertVersion;
    }

    public Version getRemoveVersion() {
        return removeVersion;
    }

    public boolean isRemoved() {
        return removeVersion != null;
    }

    // /**
    // * Doubly LinkedList
    // */
    // static class LL<T> {

    // Node head; // head of list
    // Node tail; // tail of list

    // Node node(T d){ return new Node(d);}

    // class Node {
    // T content;
    // Node prev;
    // Node next;

    // Node(T d) {
    // content = d;
    // }
    // }

    // }

    protected int height;

    protected int size;

    protected int depth;

    protected int hash;

    public boolean existsAt(Version version) {
        if (this.insertVersion == null && this.removeVersion==null) {
            return true;
        } else if (this.insertVersion == null && version == null) {
            return true;
        } else if (version == null) {
            return false;
        } else if ((this.insertVersion==null || version.compareTo(this.insertVersion) >= 0)
                && (this.removeVersion==null || version.compareTo(this.removeVersion) < 0)) {
            // insertVersion <= version < removeVersion
            return true;
        } else {
            return false;
        }
    }


    @Override
    public int getChildPosition(ITree child) {
        if (!(child instanceof AbstractVersionedTree))
            return -1;
        return getChildPosition((AbstractVersionedTree) child, ((AbstractVersionedTree) child).insertVersion);
    }

    public int getChildPosition(AbstractVersionedTree child, Version version) {
        int i = 0;
        for (AbstractVersionedTree curr : children) {
            if (curr == child) {
                return i;
            } else if (curr.existsAt(version)) {
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
            if (curr.removeVersion == null) {
                r.add(curr);
            }
        }
        return r;
    }

    public List<AbstractVersionedTree> getChildren(Version wantedVersion) {
        List<AbstractVersionedTree> r = new ArrayList<>();
        for (AbstractVersionedTree curr : children) {
            if (curr.existsAt(wantedVersion)) {
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
            throw new UnsupportedOperationException("inserted child should be an AbstractVersionedTree");
        insertChildAux((AbstractVersionedTree) child, position, ((AbstractVersionedTree) child).insertVersion,
                ((AbstractVersionedTree) child).removeVersion);
    }

    private void insertChildAux(AbstractVersionedTree child, int position, Version addedVersion,
            Version removedVersion) {
        int indexAtVersion = 0;
        for (int i = 0; i < children.size(); i++) {
            Version cAdded = children.get(i).insertVersion;
            if (indexAtVersion == position) {
                if (children.get(i).getMetadata("type").equals("LABEL") && cAdded == this.insertVersion) {
                    continue;// TODO it's mostly a trick for the linear constraint on labels
                }
                children.add(i, child);
                return;
            }
            Version cRemoved = children.get(i).removeVersion;
            if (addedVersion==null && cAdded != null) {
                continue;
            }
            if (removedVersion==null && cRemoved != null) {
                continue;
            }
            if ((cAdded == null || cAdded.compareTo(addedVersion) >= 0)
                    && (cRemoved == null || cRemoved.compareTo(removedVersion) <= 0)) {
                indexAtVersion++;
            }
        }
        if (position != indexAtVersion) {
            throw new RuntimeException(position + " " + indexAtVersion);
        }
        children.add(children.size(), child);
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

    public void delete(Version version) {
        this.removeVersion = version;
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
        // System.err.println("This method should currently not be used (please use
        // toShortString())");
        return toShortString();
    }

    @Override
    public String toShortString() {
        return String.format("%s%s%s", getMetadata("type"), SEPARATE_SYMBOL, getLabel());
        // return String.format("%s%s%s", getType(), SEPARATE_SYMBOL, getLabel());
    }

    public String toVersionedString() {
        StringBuilder b = new StringBuilder();
        if (insertVersion != null) {
            b.append(insertVersion);
        }
        if (insertVersion != null || removeVersion != null) {
            b.append("-");
        }
        if (removeVersion != null) {
            b.append(removeVersion);
        }
        if (insertVersion != null || removeVersion != null) {
            b.append(" ");
        }
        b.append(toShortString());
        return b.toString();
    }

    @Override
    public String toTreeString() {
        StringBuilder b = new StringBuilder();
        b.append(indent(this));
        b.append(toVersionedString());
        for (AbstractVersionedTree tt : getAllChildren()) {
            b.append("\n");
            b.append(tt.toTreeString());
        }
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
        public FakeTree(AbstractVersionedTree... trees) {
            this(Arrays.asList(trees));
        }
        
        public FakeTree(Collection<AbstractVersionedTree> trees) {
            children = new LinkedList<>();
            children.addAll(trees);
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

        // /**
        //  * fake nodes have no metadata
        //  */
        // @Override
        // public Object getMetadata(String key) {
        //     return null;
        // }

        // /**
        //  * fake node store no metadata
        //  */
        // @Override
        // public Object setMetadata(String key, Object value) {
        //     return null;
        // }

        // /**
        //  * Since they have no metadata they do not iterate on nothing
        //  */
        // @Override
        // public Iterator<Map.Entry<String, Object>> getMetadata() {
        //     return new EmptyEntryIterator();
        // }
        AssociationMap metadata;
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
        public Iterator<Map.Entry<String, Object>> getMetadata() {
            if (metadata == null)
                return new EmptyEntryIterator();
            return metadata.iterator();
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
