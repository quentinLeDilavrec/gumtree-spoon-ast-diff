package gumtree.spoon.apply.operations;

import spoon.reflect.declaration.CtElement;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;

public class MyCloneHelper extends CloneHelper {
    static class MyStopCloner extends CloneHelper {
        @Override
        protected Object clone() throws CloneNotSupportedException {
            return null;
        }
    }

    static MyCloneHelper.MyStopCloner STOP = new MyStopCloner();

    public <T extends CtElement> T clone(T element) {
        final CloneVisitor cloneVisitor = new CloneVisitor(STOP);
        cloneVisitor.scan(element);
        T clone = cloneVisitor.getClone();
        return clone;
    }
}