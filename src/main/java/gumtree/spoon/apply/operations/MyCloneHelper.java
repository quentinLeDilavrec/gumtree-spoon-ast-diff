package gumtree.spoon.apply.operations;

import spoon.reflect.declaration.CtElement;
import spoon.support.visitor.equals.CloneHelper;

public class MyCloneHelper extends CloneHelper {
    static class MyStopCloner extends MyCloneHelper {
        @Override
        protected Object clone() throws CloneNotSupportedException {
            return null;
        }
    }

    static MyCloneHelper.MyStopCloner SECONDARY = new MyStopCloner();

    public <T extends CtElement> T clone(T element) {
        final MyCloneVisitor cloneVisitor = new MyCloneVisitor(SECONDARY);
        cloneVisitor.scan(element);
        T clone = cloneVisitor.getClone();
        return clone;
    }
}