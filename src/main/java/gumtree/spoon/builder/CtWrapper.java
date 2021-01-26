package gumtree.spoon.builder;

import java.util.logging.Logger;

import gumtree.spoon.CloneVisitorNewFactory;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.BodyHolderSourcePosition;
import spoon.reflect.cu.position.CompoundSourcePosition;
import spoon.reflect.cu.position.DeclarationSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.path.CtPath;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.CtVisitor;
import spoon.support.reflect.CtExtendedModifier;
import spoon.support.reflect.cu.position.BodyHolderSourcePositionImpl;
import spoon.support.reflect.cu.position.CompoundSourcePositionImpl;
import spoon.support.reflect.cu.position.DeclarationSourcePositionImpl;
import spoon.support.reflect.cu.position.SourcePositionImpl;
import spoon.support.reflect.declaration.CtElementImpl;

/**
 * This class wraps objects that do not inherit from CtElement.
 * 
 * Represents Information that does not exist in the Spoon hierarchy i.e., it
 * does not inherit from CtElement, but exists somewhere in the model (e.g., an
 * attribute of a particular CtElement)
 * 
 */
public class CtWrapper<L> extends CtElementImpl {
	private static final long serialVersionUID = 8754161987319554L;

	/**
	 * The object to be wrapped
	 */
	protected L value;

	protected CtRole roleInParent;

	public CtWrapper(L wrapped, CtElement parent) {
		super();
		this.value = wrapped;
		this.parent = parent;
		setFactory(parent.getFactory());
	}

	public CtWrapper(L wrapped, CtElement parent, CtRole roleInParent) {
		super();
		this.value = wrapped;
		this.parent = parent;
		this.roleInParent = roleInParent;
		setFactory(parent.getFactory());
	}

	@Override
	public void accept(CtVisitor visitor) {
		if (visitor instanceof CloneVisitorNewFactory) {
			((CloneVisitorNewFactory) visitor).visitCtWrapper(this);
		}
	}

	@Override
	public String toString() {
		return (value != null) ? value.toString() : null;
	}

	public L getValue() {
		return value;
	}

	public void setValue(L wrapped) {
		this.value = wrapped;
	}

	@Override
	public boolean equals(Object o) {

		if (!(o instanceof CtWrapper)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		CtWrapper anotherWrap = (CtWrapper) o;
		if (this.value == null && anotherWrap.value == null)
			return true;

		return (this.value != null) && this.value.equals(anotherWrap.value);
	}

	@Override
	public CtRole getRoleInParent() {
		if (this.roleInParent != null)
			return this.roleInParent;
		return parent.getRoleInParent();
	}

	@Override
	public CtPath getPath() {
		return parent.getPath();
	}

	@Override
	public <E extends CtElement> E setPosition(SourcePosition position) {
		return super.setPosition(position);
	}

	@Override
	public SourcePosition getPosition() {
		if (super.getPosition().isValidPosition()) {
			return super.getPosition();
		}
		SourcePosition pp = parent.getPosition();
		// if (!(value instanceof CtElement) && pp instanceof DeclarationSourcePosition) {
		// 	DeclarationSourcePosition ppp = (DeclarationSourcePosition)pp;
		// 	return new SourcePositionImpl(pp.getCompilationUnit(), ppp.getNameStart(), ppp.getNameEnd(), ppp.getCompilationUnit().getLineSeparatorPositions());
		// } else 
		if (value instanceof CtElement && ((CtElement) value).getPosition().isValidPosition()) {
			return ((CtElement) value).getPosition();
		} else {
			if (value instanceof CtExtendedModifier && ((CtExtendedModifier) value).isImplicit()) {
			} else {
				Logger.getLogger("CtWrapper").warning("Should handle position specifically ; value: " + value.getClass()
						+ "`\tposition: " + pp.getClass() + "\tparent: " + parent.getClass());
			}
			return pp;
		}
	}

	public static class CtModifierWrapper extends CtWrapper<CtExtendedModifier> {

		private static final long serialVersionUID = 4198498472178613L;

		public CtModifierWrapper(CtExtendedModifier wrapped, CtElement parent) {
			super(wrapped, parent, CtRole.MODIFIER);
		}

		@Override
		public SourcePosition getPosition() {
			return value.getPosition();
		}
	}

	/**
	 * Not sure, CtRefs are already CtElements
	 */
	public static class CtRefWrapper extends CtWrapper<CtReference> {

		private static final long serialVersionUID = -89413497945L;

		public CtRefWrapper(CtNamedElement parent) {
			super(parent.getReference(), parent, CtRole.NAME);
		}

		public CtRefWrapper(CtAbstractInvocation parent) {
			super(parent.getExecutable(), parent, CtRole.EXECUTABLE_REF);
		}

		public CtRefWrapper(CtVariableAccess parent) {
			super(parent.getVariable(), parent, CtRole.VARIABLE);
		}
	}

	public static class CtNamedWrapper extends CtWrapper<CtReference> {

		private static final long serialVersionUID = 4984919891981L;

		public CtNamedWrapper(CtNamedElement parent) {
			super(parent.getReference(), parent, CtRole.NAME);
		}

		@Override
		public SourcePosition getPosition() {
			SourcePosition pp = parent.getPosition();
			if (pp instanceof DeclarationSourcePosition) {
				return makePosition((DeclarationSourcePosition) pp);
			} else {
				throw null;
			}
		}

	}

	public static SourcePositionImpl makePosition(DeclarationSourcePosition ppp) {
		return new SourcePositionImpl(ppp.getCompilationUnit(), ppp.getNameStart(), ppp.getNameEnd(),
				ppp.getCompilationUnit().getLineSeparatorPositions());
	}

	public static class CtExeRefWrapper extends CtWrapper<CtReference> {

		private static final long serialVersionUID = 2746380936937872494L;

		public CtExeRefWrapper(CtAbstractInvocation parent) {
			super(parent.getExecutable(), parent, CtRole.EXECUTABLE_REF);
		}

		@Override
		public SourcePosition getPosition() {
			SourcePosition pp = parent.getPosition();
			return new SourcePositionImpl(pp.getCompilationUnit(), pp.getSourceStart(),
					pp.getSourceStart() + value.getSimpleName().length(),
					pp.getCompilationUnit().getLineSeparatorPositions());
		}
	}

	public static class CtVarRefWrapper extends CtWrapper<CtVariableReference> {

		private static final long serialVersionUID = -8974022445909979693L;

		public CtVarRefWrapper(CtVariableAccess parent) {
			super(parent.getVariable(), parent, CtRole.NAME);
		}

		@Override
		public SourcePosition getPosition() {
			SourcePosition pp = parent.getPosition();
			return makePosition(pp, value.getSimpleName().length());
		}
	}

	public static SourcePosition makePosition(SourcePosition pp, int length) {
		if (pp instanceof CompoundSourcePosition) {
			CompoundSourcePosition p = (CompoundSourcePosition) pp;
			return new SourcePositionImpl(p.getCompilationUnit(), p.getNameStart(), p.getNameEnd(),
					p.getCompilationUnit().getLineSeparatorPositions());
		} else {
			return new SourcePositionImpl(pp.getCompilationUnit(), pp.getSourceStart(), pp.getSourceEnd(),
					pp.getCompilationUnit().getLineSeparatorPositions());
		}
	}

	public static SourcePosition makePosition(SourcePosition pp) {
		if (pp instanceof BodyHolderSourcePosition) {
			BodyHolderSourcePosition p = (BodyHolderSourcePosition) pp;
			return new BodyHolderSourcePositionImpl(p.getCompilationUnit(), p.getSourceStart(), p.getSourceEnd(),
					p.getModifierSourceStart(), p.getModifierSourceEnd(), p.getNameStart(), p.getNameEnd(),
					p.getBodyStart(), p.getBodyEnd(), p.getCompilationUnit().getLineSeparatorPositions());
		} else if (pp instanceof DeclarationSourcePosition) {
			DeclarationSourcePosition p = (DeclarationSourcePosition) pp;
			return new DeclarationSourcePositionImpl(p.getCompilationUnit(), p.getSourceStart(), p.getSourceEnd(),
					p.getModifierSourceStart(), p.getModifierSourceEnd(), p.getNameStart(), p.getNameEnd(),
					p.getCompilationUnit().getLineSeparatorPositions());
		} else if (pp instanceof CompoundSourcePosition) {
			CompoundSourcePosition p = (CompoundSourcePosition) pp;
			return new CompoundSourcePositionImpl(p.getCompilationUnit(), p.getSourceStart(), p.getSourceEnd(),
					p.getNameStart(), p.getNameEnd(), p.getCompilationUnit().getLineSeparatorPositions());
		} else if (pp instanceof SourcePosition) {
			return new SourcePositionImpl(pp.getCompilationUnit(), pp.getSourceStart(), pp.getSourceEnd(),
					pp.getCompilationUnit().getLineSeparatorPositions());
		} else {
			return pp;
		}
	}

	// TODO test those ops on positions
}
