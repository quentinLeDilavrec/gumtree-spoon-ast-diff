package gumtree.spoon.apply.operations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.actions.model.Action;

import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.OperationKind;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.reflect.declaration.CtElement;

public class ApplicableOperation<T extends Action> extends Operation<T> {
    private OperationKind kind;

    public ApplicableOperation(T action) {
        super(action);
        this.kind = getKind();
        // this.dstNode = operation.getDstNode();
        // this.srcNode = operation.getSrcNode();
    }

    @Override
    public OperationKind getKind() {
        return kind;
    }
}