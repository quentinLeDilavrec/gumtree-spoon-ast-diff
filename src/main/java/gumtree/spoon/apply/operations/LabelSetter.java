package gumtree.spoon.apply.operations;

import java.util.Arrays;

import spoon.Launcher;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.factory.Factory;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.CtIterator;

class LabelSetter extends CtInheritanceScanner {

	public final String label;

	public LabelSetter(String label) {
		this.label = label;
	}

	@Override
	public void scanCtNamedElement(CtNamedElement e) {
		e.setSimpleName(label);
	}

	@Override
	public <T> void scanCtVariableAccess(CtVariableAccess<T> variableAccess) {
		CtVariableReference<T> currentRef = variableAccess.getVariable();
		String[] splited = label.split("\\.*");
		if (splited.length == 1) {
			if (currentRef == null) {
				currentRef = variableAccess.getFactory().Core().createLocalVariableReference();
			}
			currentRef.setSimpleName(label);
			if (variableAccess instanceof CtFieldAccess) {
				CtVariableAccess<T> old = variableAccess;
				if (variableAccess instanceof CtFieldRead) {
					variableAccess = variableAccess.getFactory().Core().createVariableRead();
				} else if (variableAccess instanceof CtFieldWrite) {
					variableAccess = variableAccess.getFactory().Core().createVariableRead();
				}
				old.replace(variableAccess);
			}
		}
		
		{
			currentRef = variableAccess.getFactory().Core().createFieldReference();
			// variableAccess.getFactory().Core().createReferenceFieldReference();
		}
		variableAccess.setVariable(currentRef);
	}

	@Override
	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		invocation.setExecutable(buildExeRef(invocation));
	}

	@Override
	public <T> void visitCtConstructorCall(CtConstructorCall<T> constructorCall) {
		constructorCall.setExecutable(buildExeRef(constructorCall));
	}

	private <T> CtExecutableReference<T> buildExeRef(CtAbstractInvocation<T> constructorCall) {
		CtExecutableReference<T> element = constructorCall.getFactory().Core().createExecutableReference();
		String[] splited = label.split("\\.*");// String[] splited = label.split("\\.(?=[^\\.]+$)");
		int i = splited.length - 1;
		element.setSimpleName(splited[i]); // TODO should not rly work
		element.setDeclaringType(buildTypeRef(constructorCall.getFactory(), splited, i - 1));
		return element;
	}

	private <T> CtTypeReference<T> buildTypeRef(Factory factory, String[] splited, int i) {
		CtTypeReference<T> element = factory.Core().createTypeReference();
		element.setSimpleName(splited[i]);
		if (i > 0) {
			if (Character.isLowerCase(splited[i - 1].charAt(0))) {
				element.setDeclaringType(buildTypeRef(factory, splited, i - 1));
			} else {
				CtPackageReference pack = factory.Core().createPackageReference();
				pack.setSimpleName(String.join(".", Arrays.copyOfRange(splited, 0, i)));
				element.setPackage(pack);
			}
		}
		return element;
	}

	@Override
	public <T> void visitCtLiteral(CtLiteral<T> literal) {
		literal.getFactory().createLiteral("value");
		// Launcher o = null;
		// literal.getType().getActualClass();
		// literal.setValue("value");
		// label = literal.toString();
	}

	// @Override
	// public void visitCtIf(CtIf e) {
	// 	label = "if";
	// }

	// @Override
	// public void visitCtWhile(CtWhile e) {
	// 	label = "while";
	// }

	// @Override
	// public void visitCtBreak(CtBreak e) {
	// 	label = "break";
	// }

	// @Override
	// public void visitCtContinue(CtContinue e) {
	// 	label = "continue";
	// }

	// @Override
	// public <R> void visitCtReturn(CtReturn<R> e) {
	// 	label = "return";
	// }

	// @Override
	// public <T> void visitCtAssert(CtAssert<T> e) {
	// 	label = "assert";
	// }

	// @Override
	// public <T, A extends T> void visitCtAssignment(CtAssignment<T, A> e) {
	// 	label = "=";
	// }

	// @Override
	// public <T, A extends T> void visitCtOperatorAssignment(CtOperatorAssignment<T, A> e) {
	// 	label = e.getLabel();
	// }

	// @Override
	// public <R> void visitCtBlock(CtBlock<R> e) {
	// 	if (e.getRoleInParent() == CtRole.ELSE) {
	// 		label = "ELSE";
	// 	} else if (e.getRoleInParent() == CtRole.THEN) {
	// 		label = "THEN";
	// 	} else {
	// 		label = "{";
	// 	}
	// }

	// @Override
	// public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
	// 	label = operator.getKind().toString();
	// }

	// @Override
	// public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator) {
	// 	label = operator.getKind().toString();
	// }

	// @Override
	// public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess) {
	// 	label = thisAccess.toString();
	// }

	// @Override
	// public <T> void visitCtSuperAccess(CtSuperAccess<T> f) {
	// 	label = f.toString();
	// }

	// @Override
	// public <T> void visitCtTypeAccess(CtTypeAccess<T> typeAccess) {
	// 	if (typeAccess.getAccessedType() != null) {
	// 		label = typeAccess.getAccessedType().getQualifiedName();
	// 	}
	// }

	// @Override
	// public void visitCtComment(CtComment comment) {
	// 	label = comment.getContent();
	// }
}
