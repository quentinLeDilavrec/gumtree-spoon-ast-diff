package gumtree.spoon.diff.operations;

import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.ITree;

import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.reflect.declaration.CtElement;

public class MoveOperation extends AdditionOperation<Move> {
	public MoveOperation(Move action) {
		super(action);
	}

	@Override
	public CtElement getDstNode() {
		return (CtElement) getAction().getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
	}

	@Override
	public ITree getDst() {
		return (ITree) getAction().getNode().getMetadata(SpoonGumTreeBuilder.GT_OBJECT_DEST);
	}

	@Override
	public OperationKind getKind() {
		return OperationKind.Move;
	}

}
