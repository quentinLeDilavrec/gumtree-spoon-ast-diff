package gumtree.spoon.diff.operations;

import com.github.gumtreediff.actions.model.Addition;

import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.reflect.declaration.CtElement;

abstract class AdditionOperation<T extends Addition> extends Operation<T> {
	AdditionOperation(T action) {
		super(action);
	}

	public int getPosition() {
		return getAction().getPosition();
	}

	public CtElement getParent() {
		return (CtElement) getAction().getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((getParent() == null) ? 0 : getParent().hashCode());
		result = prime * result + getPosition();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AdditionOperation other = (AdditionOperation) obj;
		if (getParent() == null) {
			if (other.getParent() != null)
				return false;
		} else if (!getParent().equals(other.getParent()))
			return false;
		if (getPosition() != other.getPosition())
			return false;
		return true;
	}
}
