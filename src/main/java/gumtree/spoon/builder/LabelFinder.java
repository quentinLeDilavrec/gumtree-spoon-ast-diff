package gumtree.spoon.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import gumtree.spoon.apply.MyUtils;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtWhile;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.DeclarationSourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.chain.CtScannerListener;

class LabelFinder extends CtInheritanceScanner {
	public String label = "";
	public CtElement labEle = null;

	@Override
	public void scanCtNamedElement(CtNamedElement e) {
		label = e.getSimpleName();
		// labEle = new CtWrapper.CtNamedWrapper(e);
		SourcePosition pp = e.getPosition();
		if (pp instanceof DeclarationSourcePosition) {
			labEle = e.getReference();
			labEle.setPosition(CtWrapper.makePosition((DeclarationSourcePosition) pp));
		}
	}

	@Override
	public <T> void scanCtVariableAccess(CtVariableAccess<T> variableAccess) {
		label = variableAccess.getVariable().getSimpleName();
		CtVariableReference tmp = variableAccess.getVariable();
		labEle = tmp;
		labEle.setPosition(CtWrapper.makePosition(variableAccess.getPosition(), tmp.getSimpleName().length()));
	}

	@Override
	public void scanCtReference(CtReference reference) {
		if (reference instanceof CtArrayTypeReference) {
			label = ((CtArrayTypeReference) reference).getComponentType().getSimpleName();
		} else {
			label = reference.getSimpleName();
		}
		if (labEle == null) {
			labEle.setPosition(CtWrapper.makePosition(reference.getPosition(),reference.getSimpleName().length()))
		}
	}

	@Override
	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		if (invocation.getExecutable() != null) {
			CtTypeReference decl = invocation.getExecutable().getDeclaringType();
			// label =
			// (decl!=null?decl.getQualifiedName():"")+"#"+invocation.getExecutable().getSignature();
			// label = (decl != null ? decl.getQualifiedName() : "") + "#" +
			// invocation.getExecutable().getSimpleName();
			label = invocation.getExecutable().getSimpleName();
			CtExecutableReference tmp = invocation.getExecutable();
			labEle = tmp;
			labEle.setPosition(CtWrapper.makePosition(invocation.getPosition(), tmp.getSimpleName().length()));
		}
	}

	@Override
	public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
		if (ctConstructorCall.getExecutable() != null) {
			label = ctConstructorCall.getExecutable().getSimpleName();
			// label = ctConstructorCall.getExecutable().getSignature();
			CtExecutableReference tmp = ctConstructorCall.getExecutable();
			labEle = tmp;
			labEle.setPosition(CtWrapper.makePosition(ctConstructorCall.getPosition(), tmp.getSimpleName().length()));
		}
	}

	@Override
	public <T> void visitCtLiteral(CtLiteral<T> literal) {
		T val = literal.getValue();
		if (val instanceof String) {
			label = "\"" + ((String) val) + "\"";
		} else if (val instanceof Character) {
			label = "'" + ((Character) val).toString() + "'";
		} else if (val instanceof Number) {
			try {
				Class<?> c = Class.forName("spoon.reflect.visitor.LiteralHelper");
				Method m = c.getDeclaredMethod("getLiteralToken", CtLiteral.class);
				m.setAccessible(true);
				label = (String) m.invoke(null, literal);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		} else if (val != null) {
			label = val.toString();
		} else {
			label = "null";
		}
		labEle = literal;
	}

	@Override
	public <T, E extends CtExpression<?>> void visitCtExecutableReferenceExpression(
			CtExecutableReferenceExpression<T, E> e) {
		// TODO Auto-generated method stub
		label = e.getExecutable().getSimpleName();
		CtExecutableReference tmp = e.getExecutable();
		labEle = tmp;
		labEle.setPosition(CtWrapper.makePosition(e.getPosition(), tmp.getSimpleName().length()));
	}

	@Override
	public void visitCtIf(CtIf e) {
		label = "if";
	}

	@Override
	public void visitCtWhile(CtWhile e) {
		label = "while";
	}

	@Override
	public void visitCtBreak(CtBreak e) {
		label = "break";
	}

	@Override
	public void visitCtContinue(CtContinue e) {
		label = "continue";
	}

	@Override
	public <R> void visitCtReturn(CtReturn<R> e) {
		label = "return";
	}

	@Override
	public <T> void visitCtAssert(CtAssert<T> e) {
		label = "assert";
	}

	@Override
	public <T, A extends T> void visitCtAssignment(CtAssignment<T, A> e) {
		label = "=";
	}

	@Override
	public <T, A extends T> void visitCtOperatorAssignment(CtOperatorAssignment<T, A> e) {
		// label = e.getLabel();
		// if (label == null) {
		// label = MyUtils.getOperatorText(e.getKind()) + "=";
		// }
		label = e.getKind().name();
	}

	@Override
	public <R> void visitCtBlock(CtBlock<R> e) {
		if (e.getRoleInParent() == CtRole.ELSE) {
			label = "ELSE";
		} else if (e.getRoleInParent() == CtRole.THEN) {
			label = "THEN";
		} else {
			label = "{";
		}
	}

	@Override
	public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
		label = operator.getKind().toString();
	}

	@Override
	public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator) {
		label = operator.getKind().toString();
	}

	@Override
	public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess) {
		label = thisAccess.toString();
	}

	@Override
	public <T> void visitCtSuperAccess(CtSuperAccess<T> f) {
		label = f.toString();
	}

	@Override
	public <T> void visitCtTypeAccess(CtTypeAccess<T> typeAccess) {
		if (typeAccess.getAccessedType() != null) {
			label = typeAccess.getAccessedType().getQualifiedName();
			label = typeAccess.getAccessedType().getSimpleName();
		}
	}

	@Override
	public void visitCtComment(CtComment comment) {
		label = comment.getContent();
	}

	@Override
	public <A extends Annotation> void visitCtAnnotation(CtAnnotation<A> e) {
		label = e.getAnnotationType().getQualifiedName();
	}
}
