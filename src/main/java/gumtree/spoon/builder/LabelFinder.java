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
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.chain.CtScannerListener;

class LabelFinder extends CtInheritanceScanner {
	public String label = "";

	@Override
	public void scanCtNamedElement(CtNamedElement e) {
		label = e.getSimpleName();
	}

	@Override
	public <T> void scanCtVariableAccess(CtVariableAccess<T> variableAccess) {
		label = variableAccess.getVariable().getSimpleName();
	}

	@Override
	public void scanCtReference(CtReference reference) {
		if (reference instanceof CtArrayTypeReference) {
			label = ((CtArrayTypeReference) reference).getComponentType().getSimpleName();
		} else {
			label = reference.getSimpleName();
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

		}
	}

	@Override
	public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
		if (ctConstructorCall.getExecutable() != null) {
			label = ctConstructorCall.getExecutable().getSimpleName();
			// label = ctConstructorCall.getExecutable().getSignature();
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
		// 	label = MyUtils.getOperatorText(e.getKind()) + "=";
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
		label = e.getType().getQualifiedName();
	}
}
