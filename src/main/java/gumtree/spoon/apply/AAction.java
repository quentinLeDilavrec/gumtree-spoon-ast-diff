package gumtree.spoon.apply;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;

public interface AAction<T extends Action> {
    public AbstractVersionedTree getTarget();

    public ITree getSource();

    static class AMove extends Move implements AAction<Move> {

        private AbstractVersionedTree right;

        public AMove(ITree left, AbstractVersionedTree right) {
            super(left, right.getParent(), right.getParent().getChildPosition(right));
            this.right = right;
        }

        @Override
        public AbstractVersionedTree getTarget() {
            return right;
        }

        @Override
        public ITree getSource() {
            return getNode();
        }

    }

    static class AUpdate extends Update implements AAction<Update> {

        private AbstractVersionedTree right;

        public AUpdate(ITree left, AbstractVersionedTree right) {
            super(left, right.getLabel());
            //left, right.getParent(), right.getParent().getChildPosition(right)
            this.right = right;
        }

        @Override
        public AbstractVersionedTree getTarget() {
            return right;
        }

        @Override
        public ITree getSource() {
            return getNode();
        }

    }

    static class AInsert extends Insert implements AAction<Insert> {

        private ITree left;

        public AInsert(ITree left, AbstractVersionedTree right) {
            super(right, right.getParent(), right.getParent().getChildPosition(right));
            this.left = left;
        }

        @Override
        public AbstractVersionedTree getTarget() {
            return (AbstractVersionedTree) getNode();
        }

        @Override
        public ITree getSource() {
            return left;
        }

    }

    static class ADelete extends Delete implements AAction<Delete> {

        public ADelete(ITree right) {
            super(right);
        }

        @Override
        public AbstractVersionedTree getTarget() {
            return (AbstractVersionedTree) getNode();
        }

        @Override
        public ITree getSource() {
            return null;
        }

    }

    public static <T extends Action, U extends Action & AAction<T>> U build(Class<T> clazz, ITree left,
            AbstractVersionedTree right) {
        if (clazz.equals(Insert.class)) {
            return (U) new AInsert(left, right);
        } else if (clazz.equals(Delete.class)) {
            return (U) new ADelete(left);
        } else if (clazz.equals(Insert.class)) {
            return (U) new AInsert(left, right);
        } else if (clazz.equals(Insert.class)) {
            return (U) new AInsert(left, right);
        } else {
            throw new IllegalArgumentException(clazz.toString() + " not handled");
        }
    }
}