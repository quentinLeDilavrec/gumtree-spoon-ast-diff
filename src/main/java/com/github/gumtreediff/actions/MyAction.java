package com.github.gumtreediff.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;

public interface MyAction<U> {
    public String getName();
    public U getTarget();

    static List<Class> atomicActions = Arrays.asList(Insert.class, Delete.class, Update.class);

    public interface Versionable {
        Version getVersionBefore();

        Version getVersionAfter();
    }

    public interface Scripted<T extends EditScript<?>> {
        T getEditScript();
    }

    interface Scriptable<T extends EditScript<?>> extends Scripted<T> {
        void setEditScript(T es);
    }

    public interface AtomicAction<T extends ITree> extends MyAction<T> {
        /**
         * @return the node directly affected by the action
         */
        T getTarget();

        List<AtomicAction<T>> getCompressed();
    }

    interface CompressibleAtomicAction<T extends ITree> extends AtomicAction<T> {

        void setCompressed(List<AtomicAction<T>> compressed);
    }

    public interface ComposedAction<T extends ITree> extends MyAction<Set<T>> {
        default Set<T> getTarget() {
            Set<T> r = new HashSet<>();
            for (MyAction x : composed()) {
                if (x instanceof AtomicAction) {
                    r.add(((AtomicAction<T>) x).getTarget());
                } else if (x instanceof ComposedAction) {
                    r.addAll(((ComposedAction<T>) x).getTarget());
                }
            }
            return r;
        }

        List<? extends MyAction<?>> composed();
    }

    public static class MyMove extends Move
            implements ComposedAction<AbstractVersionedTree>, Scriptable<VersionedEditScript> {

        private MyDelete delete;

        public MyDelete getDelete() {
            return delete;
        }

        private MyInsert insert;

        public MyInsert getInsert() {
            return insert;
        }

        private MyMove(AbstractVersionedTree left, AbstractVersionedTree right) {
            super(left, right.getParent(), right.getParent().getChildPosition(right));
        }

        public MyMove(MyDelete delete, MyInsert insert) {
            this(delete.getTarget(), insert.getTarget());
            this.delete = delete;
            this.insert = insert;
        }

        @Override
        public List<AtomicAction<AbstractVersionedTree>> composed() {
            return Arrays.asList(delete, insert);
        }

        @Override
        public AbstractVersionedTree getNode() {
            return (AbstractVersionedTree) super.getNode();
        }

        @Override
        public AbstractVersionedTree getParent() {
            return (AbstractVersionedTree) super.getParent();
        }

        private VersionedEditScript editScript;

        @Override
        public VersionedEditScript getEditScript() {
            return this.editScript;
        }

        @Override
        public void setEditScript(VersionedEditScript editScript) {
            this.editScript = editScript;
        }
    }

    public static class MyUpdate extends Update
            implements CompressibleAtomicAction<AbstractVersionedTree>, Scriptable<VersionedEditScript> {

        private AbstractVersionedTree right;

        public MyUpdate(AbstractVersionedTree left, AbstractVersionedTree right) {
            super(left, right.getLabel());
            //left, right.getParent(), right.getParent().getChildPosition(right)
            this.right = right;
        }

        @Override
        public AbstractVersionedTree getTarget() {
            return right;
        }

        @Override
        public AbstractVersionedTree getNode() {
            return (AbstractVersionedTree) super.getNode();
        }

        private List<AtomicAction<AbstractVersionedTree>> compressed = Collections.emptyList();

        @Override
        public List<AtomicAction<AbstractVersionedTree>> getCompressed() {
            return Collections.unmodifiableList(this.compressed);
        }

        @Override
        public void setCompressed(List<AtomicAction<AbstractVersionedTree>> compressed) {
            this.compressed = compressed;
        }

        private VersionedEditScript editScript;

        @Override
        public VersionedEditScript getEditScript() {
            return this.editScript;
        }

        @Override
        public void setEditScript(VersionedEditScript editScript) {
            this.editScript = editScript;
        }

    }

    public static class MyInsert extends Insert
            implements CompressibleAtomicAction<AbstractVersionedTree>, Scriptable<VersionedEditScript> {

        private ITree left;

        public MyInsert(ITree left, AbstractVersionedTree right) {
            super(right, right.getParent(), right.getParent().getChildPosition(right));
            this.left = left;
        }

        @Override
        public AbstractVersionedTree getTarget() {
            return getNode();
        }

        @Override
        public AbstractVersionedTree getNode() {
            return (AbstractVersionedTree) super.getNode();
        }
        
        public ITree getSource() {
            return left;
        }

        @Override
        public AbstractVersionedTree getParent() {
            return (AbstractVersionedTree) super.getParent();
        }

        private List<AtomicAction<AbstractVersionedTree>> compressed = Collections.emptyList();

        @Override
        public List<AtomicAction<AbstractVersionedTree>> getCompressed() {
            return Collections.unmodifiableList(this.compressed);
        }

        @Override
        public void setCompressed(List<AtomicAction<AbstractVersionedTree>> compressed) {
            this.compressed = compressed;
        }

        private VersionedEditScript editScript;

        @Override
        public VersionedEditScript getEditScript() {
            return this.editScript;
        }

        @Override
        public void setEditScript(VersionedEditScript editScript) {
            this.editScript = editScript;
        }

    }

    public static final class MyDelete extends Delete
            implements CompressibleAtomicAction<AbstractVersionedTree>, Scriptable<VersionedEditScript> {

        public MyDelete(AbstractVersionedTree right) {
            super(right);
        }

        @Override
        public AbstractVersionedTree getTarget() {
            return (AbstractVersionedTree) getNode();
        }

        @Override
        public AbstractVersionedTree getNode() {
            return (AbstractVersionedTree) super.getNode();
        }

        private List<AtomicAction<AbstractVersionedTree>> compressed = Collections.emptyList();

        @Override
        public List<AtomicAction<AbstractVersionedTree>> getCompressed() {
            return Collections.unmodifiableList(this.compressed);
        }

        @Override
        public void setCompressed(List<AtomicAction<AbstractVersionedTree>> compressed) {
            this.compressed = compressed;
        }

        private VersionedEditScript editScript;

        @Override
        public VersionedEditScript getEditScript() {
            return this.editScript;
        }

        @Override
        public void setEditScript(VersionedEditScript editScript) {
            this.editScript = editScript;
        }
    }

    public static MyDelete invert(MyInsert action) {
        MyDelete r = new MyDelete(action.getTarget());
        r.setCompressed(new ArrayList<>(action.compressed));
        return r;
    }

    public static MyInsert invert(MyDelete action) {
        return invert(action, action.getTarget());
    }

    public static MyInsert invert(MyDelete action, AbstractVersionedTree source) {
        MyInsert r = new MyInsert(source, action.getTarget());
        r.setCompressed(new ArrayList<>(action.compressed));
        return r;
    }

    public static MyUpdate invert(MyUpdate action) {
        MyUpdate r = new MyUpdate(action.getTarget(), action.getNode());
        r.setCompressed(new ArrayList<>(action.compressed));
        return r;
    }

    public static MyMove invert(MyMove action) {
        MyMove r = new MyMove(invert(action.insert), invert(action.delete, action.insert.getTarget()));
        return r;
    }
}