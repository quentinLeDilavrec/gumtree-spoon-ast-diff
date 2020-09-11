package gumtree.spoon;

import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.CtScanner;
import spoon.support.visitor.clone.CloneBuilder;
import spoon.support.visitor.equals.CloneHelper;

/**
 * should be similar to NodeCreator
 * mostly used for insertion ?
 */
public class CloneVisitorNewFactory extends CtScanner {
	private final CloneHelper cloneHelper;

	private final CloneBuilder builder = new CloneBuilder();

	private CtElement other;

	private Factory factory;

	public CloneVisitorNewFactory(CloneHelper cloneHelper, Factory factory) {
		this.cloneHelper = cloneHelper;
		this.factory = factory;
	}

	public <T extends CtElement> T getClone() {
		return ((T) (other));
	}

	public <A extends java.lang.annotation.Annotation> void visitCtAnnotation(final CtAnnotation<A> annotation) {
		CtAnnotation<A> aCtAnnotation = this.factory.Core().createAnnotation();
		aCtAnnotation.setPosition(annotation.getPosition());
		this.builder.copy(annotation, aCtAnnotation);
		aCtAnnotation.setType(this.cloneHelper.clone(annotation.getType()));
		aCtAnnotation.setComments(this.cloneHelper.clone(annotation.getComments()));
		aCtAnnotation.setAnnotationType(this.cloneHelper.clone(annotation.getAnnotationType()));
		aCtAnnotation.setAnnotations(this.cloneHelper.clone(annotation.getAnnotations()));
		aCtAnnotation.setValues(this.cloneHelper.clone(annotation.getValues()));
		this.cloneHelper.tailor(annotation, aCtAnnotation);
		this.other = aCtAnnotation;
	}

	public <A extends java.lang.annotation.Annotation> void visitCtAnnotationType(
			final CtAnnotationType<A> annotationType) {
		CtAnnotationType<A> aCtAnnotationType = this.factory.Core().createAnnotationType();
		aCtAnnotationType.setPosition(annotationType.getPosition());
		this.builder.copy(annotationType, aCtAnnotationType);
		aCtAnnotationType.setAnnotations(this.cloneHelper.clone(annotationType.getAnnotations()));
		aCtAnnotationType.setTypeMembers(this.cloneHelper.clone(annotationType.getTypeMembers()));
		aCtAnnotationType.setComments(this.cloneHelper.clone(annotationType.getComments()));
		this.cloneHelper.tailor(annotationType, aCtAnnotationType);
		this.other = aCtAnnotationType;
	}

	public void visitCtAnonymousExecutable(final CtAnonymousExecutable anonymousExec) {
		CtAnonymousExecutable aCtAnonymousExecutable = this.factory.Core().createAnonymousExecutable();
		aCtAnonymousExecutable.setPosition(anonymousExec.getPosition());
		this.builder.copy(anonymousExec, aCtAnonymousExecutable);
		aCtAnonymousExecutable.setAnnotations(this.cloneHelper.clone(anonymousExec.getAnnotations()));
		aCtAnonymousExecutable.setBody(this.cloneHelper.clone(anonymousExec.getBody()));
		aCtAnonymousExecutable.setComments(this.cloneHelper.clone(anonymousExec.getComments()));
		this.cloneHelper.tailor(anonymousExec, aCtAnonymousExecutable);
		this.other = aCtAnonymousExecutable;
	}

	@java.lang.Override
	public <T> void visitCtArrayRead(final CtArrayRead<T> arrayRead) {
		CtArrayRead<T> aCtArrayRead = this.factory.Core().createArrayRead();
		aCtArrayRead.setPosition(arrayRead.getPosition());
		this.builder.copy(arrayRead, aCtArrayRead);
		aCtArrayRead.setAnnotations(this.cloneHelper.clone(arrayRead.getAnnotations()));
		aCtArrayRead.setType(this.cloneHelper.clone(arrayRead.getType()));
		aCtArrayRead.setTypeCasts(this.cloneHelper.clone(arrayRead.getTypeCasts()));
		aCtArrayRead.setTarget(this.cloneHelper.clone(arrayRead.getTarget()));
		aCtArrayRead.setIndexExpression(this.cloneHelper.clone(arrayRead.getIndexExpression()));
		aCtArrayRead.setComments(this.cloneHelper.clone(arrayRead.getComments()));
		this.cloneHelper.tailor(arrayRead, aCtArrayRead);
		this.other = aCtArrayRead;
	}

	@java.lang.Override
	public <T> void visitCtArrayWrite(final CtArrayWrite<T> arrayWrite) {
		CtArrayWrite<T> aCtArrayWrite = this.factory.Core().createArrayWrite();
		aCtArrayWrite.setPosition(arrayWrite.getPosition());
		this.builder.copy(arrayWrite, aCtArrayWrite);
		aCtArrayWrite.setAnnotations(this.cloneHelper.clone(arrayWrite.getAnnotations()));
		aCtArrayWrite.setType(this.cloneHelper.clone(arrayWrite.getType()));
		aCtArrayWrite.setTypeCasts(this.cloneHelper.clone(arrayWrite.getTypeCasts()));
		aCtArrayWrite.setTarget(this.cloneHelper.clone(arrayWrite.getTarget()));
		aCtArrayWrite.setIndexExpression(this.cloneHelper.clone(arrayWrite.getIndexExpression()));
		aCtArrayWrite.setComments(this.cloneHelper.clone(arrayWrite.getComments()));
		this.cloneHelper.tailor(arrayWrite, aCtArrayWrite);
		this.other = aCtArrayWrite;
	}

	public <T> void visitCtArrayTypeReference(final CtArrayTypeReference<T> reference) {
		CtArrayTypeReference<T> aCtArrayTypeReference = this.factory.Core().createArrayTypeReference();
		aCtArrayTypeReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtArrayTypeReference);
		aCtArrayTypeReference.setPackage(this.cloneHelper.clone(reference.getPackage()));
		aCtArrayTypeReference.setDeclaringType(this.cloneHelper.clone(reference.getDeclaringType()));
		aCtArrayTypeReference.setComponentType(this.cloneHelper.clone(reference.getComponentType()));
		aCtArrayTypeReference.setActualTypeArguments(this.cloneHelper.clone(reference.getActualTypeArguments()));
		aCtArrayTypeReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtArrayTypeReference);
		this.other = aCtArrayTypeReference;
	}

	public <T> void visitCtAssert(final CtAssert<T> asserted) {
		CtAssert<T> aCtAssert = this.factory.Core().createAssert();
		aCtAssert.setPosition(asserted.getPosition());
		this.builder.copy(asserted, aCtAssert);
		aCtAssert.setAnnotations(this.cloneHelper.clone(asserted.getAnnotations()));
		aCtAssert.setAssertExpression(this.cloneHelper.clone(asserted.getAssertExpression()));
		aCtAssert.setExpression(this.cloneHelper.clone(asserted.getExpression()));
		aCtAssert.setComments(this.cloneHelper.clone(asserted.getComments()));
		this.cloneHelper.tailor(asserted, aCtAssert);
		this.other = aCtAssert;
	}

	public <T, A extends T> void visitCtAssignment(final CtAssignment<T, A> assignement) {
		CtAssignment<T, A> aCtAssignment = this.factory.Core().createAssignment();
		aCtAssignment.setPosition(assignement.getPosition());
		this.builder.copy(assignement, aCtAssignment);
		aCtAssignment.setAnnotations(this.cloneHelper.clone(assignement.getAnnotations()));
		aCtAssignment.setType(this.cloneHelper.clone(assignement.getType()));
		aCtAssignment.setTypeCasts(this.cloneHelper.clone(assignement.getTypeCasts()));
		aCtAssignment.setAssigned(this.cloneHelper.clone(assignement.getAssigned()));
		aCtAssignment.setAssignment(this.cloneHelper.clone(assignement.getAssignment()));
		aCtAssignment.setComments(this.cloneHelper.clone(assignement.getComments()));
		this.cloneHelper.tailor(assignement, aCtAssignment);
		this.other = aCtAssignment;
	}

	public <T> void visitCtBinaryOperator(final CtBinaryOperator<T> operator) {
		CtBinaryOperator<T> aCtBinaryOperator = this.factory.Core().createBinaryOperator();
		aCtBinaryOperator.setPosition(operator.getPosition());
		this.builder.copy(operator, aCtBinaryOperator);
		aCtBinaryOperator.setAnnotations(this.cloneHelper.clone(operator.getAnnotations()));
		aCtBinaryOperator.setType(this.cloneHelper.clone(operator.getType()));
		aCtBinaryOperator.setTypeCasts(this.cloneHelper.clone(operator.getTypeCasts()));
		aCtBinaryOperator.setLeftHandOperand(this.cloneHelper.clone(operator.getLeftHandOperand()));
		aCtBinaryOperator.setRightHandOperand(this.cloneHelper.clone(operator.getRightHandOperand()));
		aCtBinaryOperator.setComments(this.cloneHelper.clone(operator.getComments()));
		this.cloneHelper.tailor(operator, aCtBinaryOperator);
		this.other = aCtBinaryOperator;
	}

	public <R> void visitCtBlock(final CtBlock<R> block) {
		CtBlock<R> aCtBlock = this.factory.Core().createBlock();
		aCtBlock.setPosition(block.getPosition());
		this.builder.copy(block, aCtBlock);
		aCtBlock.setAnnotations(this.cloneHelper.clone(block.getAnnotations()));
		aCtBlock.setStatements(this.cloneHelper.clone(block.getStatements()));
		aCtBlock.setComments(this.cloneHelper.clone(block.getComments()));
		this.cloneHelper.tailor(block, aCtBlock);
		this.other = aCtBlock;
	}

	public void visitCtBreak(final CtBreak breakStatement) {
		CtBreak aCtBreak = this.factory.Core().createBreak();
		aCtBreak.setPosition(breakStatement.getPosition());
		this.builder.copy(breakStatement, aCtBreak);
		aCtBreak.setAnnotations(this.cloneHelper.clone(breakStatement.getAnnotations()));
		aCtBreak.setComments(this.cloneHelper.clone(breakStatement.getComments()));
		this.cloneHelper.tailor(breakStatement, aCtBreak);
		this.other = aCtBreak;
	}

	public <S> void visitCtCase(final CtCase<S> caseStatement) {
		CtCase<S> aCtCase = this.factory.Core().createCase();
		aCtCase.setPosition(caseStatement.getPosition());
		this.builder.copy(caseStatement, aCtCase);
		aCtCase.setAnnotations(this.cloneHelper.clone(caseStatement.getAnnotations()));
		aCtCase.setCaseExpression(this.cloneHelper.clone(caseStatement.getCaseExpression()));
		aCtCase.setStatements(this.cloneHelper.clone(caseStatement.getStatements()));
		aCtCase.setComments(this.cloneHelper.clone(caseStatement.getComments()));
		this.cloneHelper.tailor(caseStatement, aCtCase);
		this.other = aCtCase;
	}

	public void visitCtCatch(final CtCatch catchBlock) {
		CtCatch aCtCatch = this.factory.Core().createCatch();
		aCtCatch.setPosition(catchBlock.getPosition());
		this.builder.copy(catchBlock, aCtCatch);
		aCtCatch.setAnnotations(this.cloneHelper.clone(catchBlock.getAnnotations()));
		aCtCatch.setParameter(this.cloneHelper.clone(catchBlock.getParameter()));
		aCtCatch.setBody(this.cloneHelper.clone(catchBlock.getBody()));
		aCtCatch.setComments(this.cloneHelper.clone(catchBlock.getComments()));
		this.cloneHelper.tailor(catchBlock, aCtCatch);
		this.other = aCtCatch;
	}

	public <T> void visitCtClass(final CtClass<T> ctClass) {
		CtClass<T> aCtClass = this.factory.Core().createClass();
		aCtClass.setPosition(ctClass.getPosition());
		this.builder.copy(ctClass, aCtClass);
		aCtClass.setAnnotations(this.cloneHelper.clone(ctClass.getAnnotations()));
		aCtClass.setSuperclass(this.cloneHelper.clone(ctClass.getSuperclass()));
		aCtClass.setSuperInterfaces(this.cloneHelper.clone(ctClass.getSuperInterfaces()));
		aCtClass.setFormalCtTypeParameters(this.cloneHelper.clone(ctClass.getFormalCtTypeParameters()));
		aCtClass.setTypeMembers(this.cloneHelper.clone(ctClass.getTypeMembers()));
		aCtClass.setComments(this.cloneHelper.clone(ctClass.getComments()));
		this.cloneHelper.tailor(ctClass, aCtClass);
		this.other = aCtClass;
	}

	@java.lang.Override
	public void visitCtTypeParameter(CtTypeParameter typeParameter) {
		CtTypeParameter aCtTypeParameter = this.factory.Core().createTypeParameter();
		aCtTypeParameter.setPosition(typeParameter.getPosition());
		this.builder.copy(typeParameter, aCtTypeParameter);
		aCtTypeParameter.setAnnotations(this.cloneHelper.clone(typeParameter.getAnnotations()));
		aCtTypeParameter.setSuperclass(this.cloneHelper.clone(typeParameter.getSuperclass()));
		aCtTypeParameter.setComments(this.cloneHelper.clone(typeParameter.getComments()));
		this.cloneHelper.tailor(typeParameter, aCtTypeParameter);
		this.other = aCtTypeParameter;
	}

	public <T> void visitCtConditional(final CtConditional<T> conditional) {
		CtConditional<T> aCtConditional = this.factory.Core().createConditional();
		aCtConditional.setPosition(conditional.getPosition());
		this.builder.copy(conditional, aCtConditional);
		aCtConditional.setType(this.cloneHelper.clone(conditional.getType()));
		aCtConditional.setAnnotations(this.cloneHelper.clone(conditional.getAnnotations()));
		aCtConditional.setCondition(this.cloneHelper.clone(conditional.getCondition()));
		aCtConditional.setThenExpression(this.cloneHelper.clone(conditional.getThenExpression()));
		aCtConditional.setElseExpression(this.cloneHelper.clone(conditional.getElseExpression()));
		aCtConditional.setComments(this.cloneHelper.clone(conditional.getComments()));
		aCtConditional.setTypeCasts(this.cloneHelper.clone(conditional.getTypeCasts()));
		this.cloneHelper.tailor(conditional, aCtConditional);
		this.other = aCtConditional;
	}

	public <T> void visitCtConstructor(final CtConstructor<T> c) {
		CtConstructor<T> aCtConstructor = this.factory.Core().createConstructor();
		aCtConstructor.setPosition(c.getPosition());
		this.builder.copy(c, aCtConstructor);
		aCtConstructor.setAnnotations(this.cloneHelper.clone(c.getAnnotations()));
		aCtConstructor.setParameters(this.cloneHelper.clone(c.getParameters()));
		aCtConstructor.setThrownTypes(this.cloneHelper.clone(c.getThrownTypes()));
		aCtConstructor.setFormalCtTypeParameters(this.cloneHelper.clone(c.getFormalCtTypeParameters()));
		aCtConstructor.setBody(this.cloneHelper.clone(c.getBody()));
		aCtConstructor.setComments(this.cloneHelper.clone(c.getComments()));
		this.cloneHelper.tailor(c, aCtConstructor);
		this.other = aCtConstructor;
	}

	public void visitCtContinue(final CtContinue continueStatement) {
		CtContinue aCtContinue = this.factory.Core().createContinue();
		aCtContinue.setPosition(continueStatement.getPosition());
		this.builder.copy(continueStatement, aCtContinue);
		aCtContinue.setAnnotations(this.cloneHelper.clone(continueStatement.getAnnotations()));
		aCtContinue.setComments(this.cloneHelper.clone(continueStatement.getComments()));
		this.cloneHelper.tailor(continueStatement, aCtContinue);
		this.other = aCtContinue;
	}

	public void visitCtDo(final CtDo doLoop) {
		CtDo aCtDo = this.factory.Core().createDo();
		aCtDo.setPosition(doLoop.getPosition());
		this.builder.copy(doLoop, aCtDo);
		aCtDo.setAnnotations(this.cloneHelper.clone(doLoop.getAnnotations()));
		aCtDo.setLoopingExpression(this.cloneHelper.clone(doLoop.getLoopingExpression()));
		aCtDo.setBody(this.cloneHelper.clone(doLoop.getBody()));
		aCtDo.setComments(this.cloneHelper.clone(doLoop.getComments()));
		this.cloneHelper.tailor(doLoop, aCtDo);
		this.other = aCtDo;
	}

	public <T extends java.lang.Enum<?>> void visitCtEnum(final CtEnum<T> ctEnum) {
		CtEnum<T> aCtEnum = this.factory.Core().createEnum();
		aCtEnum.setPosition(ctEnum.getPosition());
		this.builder.copy(ctEnum, aCtEnum);
		aCtEnum.setAnnotations(this.cloneHelper.clone(ctEnum.getAnnotations()));
		aCtEnum.setSuperInterfaces(this.cloneHelper.clone(ctEnum.getSuperInterfaces()));
		aCtEnum.setTypeMembers(this.cloneHelper.clone(ctEnum.getTypeMembers()));
		aCtEnum.setEnumValues(this.cloneHelper.clone(ctEnum.getEnumValues()));
		aCtEnum.setComments(this.cloneHelper.clone(ctEnum.getComments()));
		this.cloneHelper.tailor(ctEnum, aCtEnum);
		this.other = aCtEnum;
	}

	public <T> void visitCtExecutableReference(final CtExecutableReference<T> reference) {
		CtExecutableReference<T> aCtExecutableReference = this.factory.Core().createExecutableReference();
		aCtExecutableReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtExecutableReference);
		aCtExecutableReference.setDeclaringType(this.cloneHelper.clone(reference.getDeclaringType()));
		aCtExecutableReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtExecutableReference.setParameters(this.cloneHelper.clone(reference.getParameters()));
		aCtExecutableReference.setActualTypeArguments(this.cloneHelper.clone(reference.getActualTypeArguments()));
		aCtExecutableReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		aCtExecutableReference.setComments(this.cloneHelper.clone(reference.getComments()));
		this.cloneHelper.tailor(reference, aCtExecutableReference);
		this.other = aCtExecutableReference;
	}

	public <T> void visitCtField(final CtField<T> f) {
		CtField<T> aCtField = this.factory.Core().createField();
		aCtField.setPosition(f.getPosition());
		this.builder.copy(f, aCtField);
		aCtField.setAnnotations(this.cloneHelper.clone(f.getAnnotations()));
		aCtField.setType(this.cloneHelper.clone(f.getType()));
		aCtField.setDefaultExpression(this.cloneHelper.clone(f.getDefaultExpression()));
		aCtField.setComments(this.cloneHelper.clone(f.getComments()));
		this.cloneHelper.tailor(f, aCtField);
		this.other = aCtField;
	}

	@java.lang.Override
	public <T> void visitCtEnumValue(final CtEnumValue<T> enumValue) {
		CtEnumValue<T> aCtEnumValue = this.factory.Core().createEnumValue();
		aCtEnumValue.setPosition(enumValue.getPosition());
		this.builder.copy(enumValue, aCtEnumValue);
		aCtEnumValue.setAnnotations(this.cloneHelper.clone(enumValue.getAnnotations()));
		aCtEnumValue.setType(this.cloneHelper.clone(enumValue.getType()));
		aCtEnumValue.setDefaultExpression(this.cloneHelper.clone(enumValue.getDefaultExpression()));
		aCtEnumValue.setComments(this.cloneHelper.clone(enumValue.getComments()));
		this.cloneHelper.tailor(enumValue, aCtEnumValue);
		this.other = aCtEnumValue;
	}

	@java.lang.Override
	public <T> void visitCtThisAccess(final CtThisAccess<T> thisAccess) {
		CtThisAccess<T> aCtThisAccess = this.factory.Core().createThisAccess();
		aCtThisAccess.setPosition(thisAccess.getPosition());
		this.builder.copy(thisAccess, aCtThisAccess);
		aCtThisAccess.setComments(this.cloneHelper.clone(thisAccess.getComments()));
		aCtThisAccess.setAnnotations(this.cloneHelper.clone(thisAccess.getAnnotations()));
		aCtThisAccess.setType(this.cloneHelper.clone(thisAccess.getType()));
		aCtThisAccess.setTypeCasts(this.cloneHelper.clone(thisAccess.getTypeCasts()));
		aCtThisAccess.setTarget(this.cloneHelper.clone(thisAccess.getTarget()));
		this.cloneHelper.tailor(thisAccess, aCtThisAccess);
		this.other = aCtThisAccess;
	}

	public <T> void visitCtAnnotationFieldAccess(final CtAnnotationFieldAccess<T> annotationFieldAccess) {
		CtAnnotationFieldAccess<T> aCtAnnotationFieldAccess = this.factory.Core().createAnnotationFieldAccess();
		aCtAnnotationFieldAccess.setPosition(annotationFieldAccess.getPosition());
		this.builder.copy(annotationFieldAccess, aCtAnnotationFieldAccess);
		aCtAnnotationFieldAccess.setComments(this.cloneHelper.clone(annotationFieldAccess.getComments()));
		aCtAnnotationFieldAccess.setAnnotations(this.cloneHelper.clone(annotationFieldAccess.getAnnotations()));
		aCtAnnotationFieldAccess.setTypeCasts(this.cloneHelper.clone(annotationFieldAccess.getTypeCasts()));
		aCtAnnotationFieldAccess.setTarget(this.cloneHelper.clone(annotationFieldAccess.getTarget()));
		aCtAnnotationFieldAccess.setVariable(this.cloneHelper.clone(annotationFieldAccess.getVariable()));
		this.cloneHelper.tailor(annotationFieldAccess, aCtAnnotationFieldAccess);
		this.other = aCtAnnotationFieldAccess;
	}

	public <T> void visitCtFieldReference(final CtFieldReference<T> reference) {
		CtFieldReference<T> aCtFieldReference = this.factory.Core().createFieldReference();
		aCtFieldReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtFieldReference);
		aCtFieldReference.setDeclaringType(this.cloneHelper.clone(reference.getDeclaringType()));
		aCtFieldReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtFieldReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtFieldReference);
		this.other = aCtFieldReference;
	}

	public void visitCtFor(final CtFor forLoop) {
		CtFor aCtFor = this.factory.Core().createFor();
		aCtFor.setPosition(forLoop.getPosition());
		this.builder.copy(forLoop, aCtFor);
		aCtFor.setAnnotations(this.cloneHelper.clone(forLoop.getAnnotations()));
		aCtFor.setForInit(this.cloneHelper.clone(forLoop.getForInit()));
		aCtFor.setExpression(this.cloneHelper.clone(forLoop.getExpression()));
		aCtFor.setForUpdate(this.cloneHelper.clone(forLoop.getForUpdate()));
		aCtFor.setBody(this.cloneHelper.clone(forLoop.getBody()));
		aCtFor.setComments(this.cloneHelper.clone(forLoop.getComments()));
		this.cloneHelper.tailor(forLoop, aCtFor);
		this.other = aCtFor;
	}

	public void visitCtForEach(final CtForEach foreach) {
		CtForEach aCtForEach = this.factory.Core().createForEach();
		aCtForEach.setPosition(foreach.getPosition());
		this.builder.copy(foreach, aCtForEach);
		aCtForEach.setAnnotations(this.cloneHelper.clone(foreach.getAnnotations()));
		aCtForEach.setVariable(this.cloneHelper.clone(foreach.getVariable()));
		aCtForEach.setExpression(this.cloneHelper.clone(foreach.getExpression()));
		aCtForEach.setBody(this.cloneHelper.clone(foreach.getBody()));
		aCtForEach.setComments(this.cloneHelper.clone(foreach.getComments()));
		this.cloneHelper.tailor(foreach, aCtForEach);
		this.other = aCtForEach;
	}

	public void visitCtIf(final CtIf ifElement) {
		CtIf aCtIf = this.factory.Core().createIf();
		aCtIf.setPosition(ifElement.getPosition());
		this.builder.copy(ifElement, aCtIf);
		aCtIf.setAnnotations(this.cloneHelper.clone(ifElement.getAnnotations()));
		aCtIf.setCondition(this.cloneHelper.clone(ifElement.getCondition()));
		aCtIf.setThenStatement(this.cloneHelper.clone(((CtStatement) (ifElement.getThenStatement()))));
		aCtIf.setElseStatement(this.cloneHelper.clone(((CtStatement) (ifElement.getElseStatement()))));
		aCtIf.setComments(this.cloneHelper.clone(ifElement.getComments()));
		this.cloneHelper.tailor(ifElement, aCtIf);
		this.other = aCtIf;
	}

	public <T> void visitCtInterface(final CtInterface<T> intrface) {
		CtInterface<T> aCtInterface = this.factory.Core().createInterface();
		aCtInterface.setPosition(intrface.getPosition());
		this.builder.copy(intrface, aCtInterface);
		aCtInterface.setAnnotations(this.cloneHelper.clone(intrface.getAnnotations()));
		aCtInterface.setSuperInterfaces(this.cloneHelper.clone(intrface.getSuperInterfaces()));
		aCtInterface.setFormalCtTypeParameters(this.cloneHelper.clone(intrface.getFormalCtTypeParameters()));
		aCtInterface.setTypeMembers(this.cloneHelper.clone(intrface.getTypeMembers()));
		aCtInterface.setComments(this.cloneHelper.clone(intrface.getComments()));
		this.cloneHelper.tailor(intrface, aCtInterface);
		this.other = aCtInterface;
	}

	public <T> void visitCtInvocation(final CtInvocation<T> invocation) {
		CtInvocation<T> aCtInvocation = this.factory.Core().createInvocation();
		aCtInvocation.setPosition(invocation.getPosition());
		this.builder.copy(invocation, aCtInvocation);
		aCtInvocation.setAnnotations(this.cloneHelper.clone(invocation.getAnnotations()));
		aCtInvocation.setTypeCasts(this.cloneHelper.clone(invocation.getTypeCasts()));
		aCtInvocation.setTarget(this.cloneHelper.clone(invocation.getTarget()));
		aCtInvocation.setExecutable(this.cloneHelper.clone(invocation.getExecutable()));
		aCtInvocation.setArguments(this.cloneHelper.clone(invocation.getArguments()));
		aCtInvocation.setComments(this.cloneHelper.clone(invocation.getComments()));
		this.cloneHelper.tailor(invocation, aCtInvocation);
		this.other = aCtInvocation;
	}

	public <T> void visitCtLiteral(final CtLiteral<T> literal) {
		CtLiteral<T> aCtLiteral = this.factory.Core().createLiteral();
		aCtLiteral.setPosition(literal.getPosition());
		this.builder.copy(literal, aCtLiteral);
		aCtLiteral.setAnnotations(this.cloneHelper.clone(literal.getAnnotations()));
		aCtLiteral.setType(this.cloneHelper.clone(literal.getType()));
		aCtLiteral.setTypeCasts(this.cloneHelper.clone(literal.getTypeCasts()));
		aCtLiteral.setComments(this.cloneHelper.clone(literal.getComments()));
		this.cloneHelper.tailor(literal, aCtLiteral);
		this.other = aCtLiteral;
	}

	public <T> void visitCtLocalVariable(final CtLocalVariable<T> localVariable) {
		CtLocalVariable<T> aCtLocalVariable = this.factory.Core().createLocalVariable();
		aCtLocalVariable.setPosition(localVariable.getPosition());
		this.builder.copy(localVariable, aCtLocalVariable);
		aCtLocalVariable.setAnnotations(this.cloneHelper.clone(localVariable.getAnnotations()));
		aCtLocalVariable.setType(this.cloneHelper.clone(localVariable.getType()));
		aCtLocalVariable.setDefaultExpression(this.cloneHelper.clone(localVariable.getDefaultExpression()));
		aCtLocalVariable.setComments(this.cloneHelper.clone(localVariable.getComments()));
		this.cloneHelper.tailor(localVariable, aCtLocalVariable);
		this.other = aCtLocalVariable;
	}

	public <T> void visitCtLocalVariableReference(final CtLocalVariableReference<T> reference) {
		CtLocalVariableReference<T> aCtLocalVariableReference = this.factory.Core().createLocalVariableReference();
		aCtLocalVariableReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtLocalVariableReference);
		aCtLocalVariableReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtLocalVariableReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtLocalVariableReference);
		this.other = aCtLocalVariableReference;
	}

	public <T> void visitCtCatchVariable(final CtCatchVariable<T> catchVariable) {
		CtCatchVariable<T> aCtCatchVariable = this.factory.Core().createCatchVariable();
		aCtCatchVariable.setPosition(catchVariable.getPosition());
		this.builder.copy(catchVariable, aCtCatchVariable);
		aCtCatchVariable.setComments(this.cloneHelper.clone(catchVariable.getComments()));
		aCtCatchVariable.setAnnotations(this.cloneHelper.clone(catchVariable.getAnnotations()));
		aCtCatchVariable.setMultiTypes(this.cloneHelper.clone(catchVariable.getMultiTypes()));
		this.cloneHelper.tailor(catchVariable, aCtCatchVariable);
		this.other = aCtCatchVariable;
	}

	public <T> void visitCtCatchVariableReference(final CtCatchVariableReference<T> reference) {
		CtCatchVariableReference<T> aCtCatchVariableReference = this.factory.Core().createCatchVariableReference();
		aCtCatchVariableReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtCatchVariableReference);
		aCtCatchVariableReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtCatchVariableReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtCatchVariableReference);
		this.other = aCtCatchVariableReference;
	}

	public <T> void visitCtMethod(final CtMethod<T> m) {
		CtMethod<T> aCtMethod = this.factory.Core().createMethod();
		aCtMethod.setPosition(m.getPosition());
		this.builder.copy(m, aCtMethod);
		aCtMethod.setAnnotations(this.cloneHelper.clone(m.getAnnotations()));
		aCtMethod.setFormalCtTypeParameters(this.cloneHelper.clone(m.getFormalCtTypeParameters()));
		aCtMethod.setType(this.cloneHelper.clone(m.getType()));
		aCtMethod.setParameters(this.cloneHelper.clone(m.getParameters()));
		aCtMethod.setThrownTypes(this.cloneHelper.clone(m.getThrownTypes()));
		aCtMethod.setBody(this.cloneHelper.clone(m.getBody()));
		aCtMethod.setComments(this.cloneHelper.clone(m.getComments()));
		this.cloneHelper.tailor(m, aCtMethod);
		this.other = aCtMethod;
	}

	@java.lang.Override
	public <T> void visitCtAnnotationMethod(CtAnnotationMethod<T> annotationMethod) {
		CtAnnotationMethod<T> aCtAnnotationMethod = this.factory.Core().createAnnotationMethod();
		aCtAnnotationMethod.setPosition(annotationMethod.getPosition());
		this.builder.copy(annotationMethod, aCtAnnotationMethod);
		aCtAnnotationMethod.setAnnotations(this.cloneHelper.clone(annotationMethod.getAnnotations()));
		aCtAnnotationMethod.setType(this.cloneHelper.clone(annotationMethod.getType()));
		aCtAnnotationMethod.setDefaultExpression(this.cloneHelper.clone(annotationMethod.getDefaultExpression()));
		aCtAnnotationMethod.setComments(this.cloneHelper.clone(annotationMethod.getComments()));
		this.cloneHelper.tailor(annotationMethod, aCtAnnotationMethod);
		this.other = aCtAnnotationMethod;
	}

	public <T> void visitCtNewArray(final CtNewArray<T> newArray) {
		CtNewArray<T> aCtNewArray = this.factory.Core().createNewArray();
		aCtNewArray.setPosition(newArray.getPosition());
		this.builder.copy(newArray, aCtNewArray);
		aCtNewArray.setAnnotations(this.cloneHelper.clone(newArray.getAnnotations()));
		aCtNewArray.setType(this.cloneHelper.clone(newArray.getType()));
		aCtNewArray.setTypeCasts(this.cloneHelper.clone(newArray.getTypeCasts()));
		aCtNewArray.setElements(this.cloneHelper.clone(newArray.getElements()));
		aCtNewArray.setDimensionExpressions(this.cloneHelper.clone(newArray.getDimensionExpressions()));
		aCtNewArray.setComments(this.cloneHelper.clone(newArray.getComments()));
		this.cloneHelper.tailor(newArray, aCtNewArray);
		this.other = aCtNewArray;
	}

	@java.lang.Override
	public <T> void visitCtConstructorCall(final CtConstructorCall<T> ctConstructorCall) {
		CtConstructorCall<T> aCtConstructorCall = this.factory.Core().createConstructorCall();
		aCtConstructorCall.setPosition(ctConstructorCall.getPosition());
		this.builder.copy(ctConstructorCall, aCtConstructorCall);
		aCtConstructorCall.setAnnotations(this.cloneHelper.clone(ctConstructorCall.getAnnotations()));
		aCtConstructorCall.setTypeCasts(this.cloneHelper.clone(ctConstructorCall.getTypeCasts()));
		aCtConstructorCall.setExecutable(this.cloneHelper.clone(ctConstructorCall.getExecutable()));
		aCtConstructorCall.setTarget(this.cloneHelper.clone(ctConstructorCall.getTarget()));
		aCtConstructorCall.setArguments(this.cloneHelper.clone(ctConstructorCall.getArguments()));
		aCtConstructorCall.setComments(this.cloneHelper.clone(ctConstructorCall.getComments()));
		this.cloneHelper.tailor(ctConstructorCall, aCtConstructorCall);
		this.other = aCtConstructorCall;
	}

	public <T> void visitCtNewClass(final CtNewClass<T> newClass) {
		CtNewClass<T> aCtNewClass = this.factory.Core().createNewClass();
		aCtNewClass.setPosition(newClass.getPosition());
		this.builder.copy(newClass, aCtNewClass);
		aCtNewClass.setAnnotations(this.cloneHelper.clone(newClass.getAnnotations()));
		aCtNewClass.setTypeCasts(this.cloneHelper.clone(newClass.getTypeCasts()));
		aCtNewClass.setExecutable(this.cloneHelper.clone(newClass.getExecutable()));
		aCtNewClass.setTarget(this.cloneHelper.clone(newClass.getTarget()));
		aCtNewClass.setArguments(this.cloneHelper.clone(newClass.getArguments()));
		aCtNewClass.setAnonymousClass(this.cloneHelper.clone(newClass.getAnonymousClass()));
		aCtNewClass.setComments(this.cloneHelper.clone(newClass.getComments()));
		this.cloneHelper.tailor(newClass, aCtNewClass);
		this.other = aCtNewClass;
	}

	@java.lang.Override
	public <T> void visitCtLambda(final CtLambda<T> lambda) {
		CtLambda<T> aCtLambda = this.factory.Core().createLambda();
		aCtLambda.setPosition(lambda.getPosition());
		this.builder.copy(lambda, aCtLambda);
		aCtLambda.setAnnotations(this.cloneHelper.clone(lambda.getAnnotations()));
		aCtLambda.setType(this.cloneHelper.clone(lambda.getType()));
		aCtLambda.setTypeCasts(this.cloneHelper.clone(lambda.getTypeCasts()));
		aCtLambda.setParameters(this.cloneHelper.clone(lambda.getParameters()));
		aCtLambda.setBody(this.cloneHelper.clone(lambda.getBody()));
		aCtLambda.setExpression(this.cloneHelper.clone(lambda.getExpression()));
		aCtLambda.setComments(this.cloneHelper.clone(lambda.getComments()));
		this.cloneHelper.tailor(lambda, aCtLambda);
		this.other = aCtLambda;
	}

	@java.lang.Override
	public <T, E extends CtExpression<?>> void visitCtExecutableReferenceExpression(
			final CtExecutableReferenceExpression<T, E> expression) {
		CtExecutableReferenceExpression<T, E> aCtExecutableReferenceExpression = this.factory.Core().createExecutableReferenceExpression();
		aCtExecutableReferenceExpression.setPosition(expression.getPosition());
		this.builder.copy(expression, aCtExecutableReferenceExpression);
		aCtExecutableReferenceExpression.setComments(this.cloneHelper.clone(expression.getComments()));
		aCtExecutableReferenceExpression.setAnnotations(this.cloneHelper.clone(expression.getAnnotations()));
		aCtExecutableReferenceExpression.setType(this.cloneHelper.clone(expression.getType()));
		aCtExecutableReferenceExpression.setTypeCasts(this.cloneHelper.clone(expression.getTypeCasts()));
		aCtExecutableReferenceExpression.setExecutable(this.cloneHelper.clone(expression.getExecutable()));
		aCtExecutableReferenceExpression.setTarget(this.cloneHelper.clone(expression.getTarget()));
		this.cloneHelper.tailor(expression, aCtExecutableReferenceExpression);
		this.other = aCtExecutableReferenceExpression;
	}

	public <T, A extends T> void visitCtOperatorAssignment(final CtOperatorAssignment<T, A> assignment) {
		CtOperatorAssignment<T, A> aCtOperatorAssignment = this.factory.Core().createOperatorAssignment();
		aCtOperatorAssignment.setPosition(assignment.getPosition());
		this.builder.copy(assignment, aCtOperatorAssignment);
		aCtOperatorAssignment.setAnnotations(this.cloneHelper.clone(assignment.getAnnotations()));
		aCtOperatorAssignment.setType(this.cloneHelper.clone(assignment.getType()));
		aCtOperatorAssignment.setTypeCasts(this.cloneHelper.clone(assignment.getTypeCasts()));
		aCtOperatorAssignment.setAssigned(this.cloneHelper.clone(assignment.getAssigned()));
		aCtOperatorAssignment.setAssignment(this.cloneHelper.clone(assignment.getAssignment()));
		aCtOperatorAssignment.setComments(this.cloneHelper.clone(assignment.getComments()));
		this.cloneHelper.tailor(assignment, aCtOperatorAssignment);
		this.other = aCtOperatorAssignment;
	}

	public void visitCtPackage(final CtPackage ctPackage) {
		CtPackage aCtPackage = this.factory.Core().createPackage();
		aCtPackage.setPosition(ctPackage.getPosition());
		this.builder.copy(ctPackage, aCtPackage);
		aCtPackage.setAnnotations(this.cloneHelper.clone(ctPackage.getAnnotations()));
		aCtPackage.setPackages(this.cloneHelper.clone(ctPackage.getPackages()));
		aCtPackage.setTypes(this.cloneHelper.clone(ctPackage.getTypes()));
		aCtPackage.setComments(this.cloneHelper.clone(ctPackage.getComments()));
		this.cloneHelper.tailor(ctPackage, aCtPackage);
		this.other = aCtPackage;
	}

	public void visitCtPackageReference(final CtPackageReference reference) {
		CtPackageReference aCtPackageReference = this.factory.Core().createPackageReference();
		aCtPackageReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtPackageReference);
		aCtPackageReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtPackageReference);
		this.other = aCtPackageReference;
	}

	public <T> void visitCtParameter(final CtParameter<T> parameter) {
		CtParameter<T> aCtParameter = this.factory.Core().createParameter();
		aCtParameter.setPosition(parameter.getPosition());
		this.builder.copy(parameter, aCtParameter);
		aCtParameter.setAnnotations(this.cloneHelper.clone(parameter.getAnnotations()));
		aCtParameter.setType(this.cloneHelper.clone(parameter.getType()));
		aCtParameter.setComments(this.cloneHelper.clone(parameter.getComments()));
		this.cloneHelper.tailor(parameter, aCtParameter);
		this.other = aCtParameter;
	}

	public <T> void visitCtParameterReference(final CtParameterReference<T> reference) {
		CtParameterReference<T> aCtParameterReference = this.factory.Core().createParameterReference();
		aCtParameterReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtParameterReference);
		aCtParameterReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtParameterReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtParameterReference);
		this.other = aCtParameterReference;
	}

	public <R> void visitCtReturn(final CtReturn<R> returnStatement) {
		CtReturn<R> aCtReturn = this.factory.Core().createReturn();
		aCtReturn.setPosition(returnStatement.getPosition());
		this.builder.copy(returnStatement, aCtReturn);
		aCtReturn.setAnnotations(this.cloneHelper.clone(returnStatement.getAnnotations()));
		aCtReturn.setReturnedExpression(this.cloneHelper.clone(returnStatement.getReturnedExpression()));
		aCtReturn.setComments(this.cloneHelper.clone(returnStatement.getComments()));
		this.cloneHelper.tailor(returnStatement, aCtReturn);
		this.other = aCtReturn;
	}

	public <R> void visitCtStatementList(final CtStatementList statements) {
		CtStatementList aCtStatementList = this.factory.Core().createStatementList();
		aCtStatementList.setPosition(statements.getPosition());
		this.builder.copy(statements, aCtStatementList);
		aCtStatementList.setAnnotations(this.cloneHelper.clone(statements.getAnnotations()));
		aCtStatementList.setStatements(this.cloneHelper.clone(statements.getStatements()));
		aCtStatementList.setComments(this.cloneHelper.clone(statements.getComments()));
		this.cloneHelper.tailor(statements, aCtStatementList);
		this.other = aCtStatementList;
	}

	public <S> void visitCtSwitch(final CtSwitch<S> switchStatement) {
		CtSwitch<S> aCtSwitch = this.factory.Core().createSwitch();
		aCtSwitch.setPosition(switchStatement.getPosition());
		this.builder.copy(switchStatement, aCtSwitch);
		aCtSwitch.setAnnotations(this.cloneHelper.clone(switchStatement.getAnnotations()));
		aCtSwitch.setSelector(this.cloneHelper.clone(switchStatement.getSelector()));
		aCtSwitch.setCases(this.cloneHelper.clone(switchStatement.getCases()));
		aCtSwitch.setComments(this.cloneHelper.clone(switchStatement.getComments()));
		this.cloneHelper.tailor(switchStatement, aCtSwitch);
		this.other = aCtSwitch;
	}

	public void visitCtSynchronized(final CtSynchronized synchro) {
		CtSynchronized aCtSynchronized = this.factory.Core().createSynchronized();
		aCtSynchronized.setPosition(synchro.getPosition());
		this.builder.copy(synchro, aCtSynchronized);
		aCtSynchronized.setAnnotations(this.cloneHelper.clone(synchro.getAnnotations()));
		aCtSynchronized.setExpression(this.cloneHelper.clone(synchro.getExpression()));
		aCtSynchronized.setBlock(this.cloneHelper.clone(synchro.getBlock()));
		aCtSynchronized.setComments(this.cloneHelper.clone(synchro.getComments()));
		this.cloneHelper.tailor(synchro, aCtSynchronized);
		this.other = aCtSynchronized;
	}

	public void visitCtThrow(final CtThrow throwStatement) {
		CtThrow aCtThrow = this.factory.Core().createThrow();
		aCtThrow.setPosition(throwStatement.getPosition());
		this.builder.copy(throwStatement, aCtThrow);
		aCtThrow.setAnnotations(this.cloneHelper.clone(throwStatement.getAnnotations()));
		aCtThrow.setThrownExpression(this.cloneHelper.clone(throwStatement.getThrownExpression()));
		aCtThrow.setComments(this.cloneHelper.clone(throwStatement.getComments()));
		this.cloneHelper.tailor(throwStatement, aCtThrow);
		this.other = aCtThrow;
	}

	public void visitCtTry(final CtTry tryBlock) {
		CtTry aCtTry = this.factory.Core().createTry();
		aCtTry.setPosition(tryBlock.getPosition());
		this.builder.copy(tryBlock, aCtTry);
		aCtTry.setAnnotations(this.cloneHelper.clone(tryBlock.getAnnotations()));
		aCtTry.setBody(this.cloneHelper.clone(tryBlock.getBody()));
		aCtTry.setCatchers(this.cloneHelper.clone(tryBlock.getCatchers()));
		aCtTry.setFinalizer(this.cloneHelper.clone(tryBlock.getFinalizer()));
		aCtTry.setComments(this.cloneHelper.clone(tryBlock.getComments()));
		this.cloneHelper.tailor(tryBlock, aCtTry);
		this.other = aCtTry;
	}

	@java.lang.Override
	public void visitCtTryWithResource(final CtTryWithResource tryWithResource) {
		CtTryWithResource aCtTryWithResource = this.factory.Core().createTryWithResource();
		aCtTryWithResource.setPosition(tryWithResource.getPosition());
		this.builder.copy(tryWithResource, aCtTryWithResource);
		aCtTryWithResource.setAnnotations(this.cloneHelper.clone(tryWithResource.getAnnotations()));
		aCtTryWithResource.setResources(this.cloneHelper.clone(tryWithResource.getResources()));
		aCtTryWithResource.setBody(this.cloneHelper.clone(tryWithResource.getBody()));
		aCtTryWithResource.setCatchers(this.cloneHelper.clone(tryWithResource.getCatchers()));
		aCtTryWithResource.setFinalizer(this.cloneHelper.clone(tryWithResource.getFinalizer()));
		aCtTryWithResource.setComments(this.cloneHelper.clone(tryWithResource.getComments()));
		this.cloneHelper.tailor(tryWithResource, aCtTryWithResource);
		this.other = aCtTryWithResource;
	}

	public void visitCtTypeParameterReference(final CtTypeParameterReference ref) {
		CtTypeParameterReference aCtTypeParameterReference = this.factory.Core().createTypeParameterReference();
		aCtTypeParameterReference.setPosition(ref.getPosition());
		this.builder.copy(ref, aCtTypeParameterReference);
		aCtTypeParameterReference.setPackage(this.cloneHelper.clone(ref.getPackage()));
		aCtTypeParameterReference.setDeclaringType(this.cloneHelper.clone(ref.getDeclaringType()));
		aCtTypeParameterReference.setAnnotations(this.cloneHelper.clone(ref.getAnnotations()));
		this.cloneHelper.tailor(ref, aCtTypeParameterReference);
		this.other = aCtTypeParameterReference;
	}

	@java.lang.Override
	public void visitCtWildcardReference(CtWildcardReference wildcardReference) {
		CtWildcardReference aCtWildcardReference = this.factory.Core().createWildcardReference();
		aCtWildcardReference.setPosition(wildcardReference.getPosition());
		this.builder.copy(wildcardReference, aCtWildcardReference);
		aCtWildcardReference.setPackage(this.cloneHelper.clone(wildcardReference.getPackage()));
		aCtWildcardReference.setDeclaringType(this.cloneHelper.clone(wildcardReference.getDeclaringType()));
		aCtWildcardReference.setAnnotations(this.cloneHelper.clone(wildcardReference.getAnnotations()));
		aCtWildcardReference.setBoundingType(this.cloneHelper.clone(wildcardReference.getBoundingType()));
		this.cloneHelper.tailor(wildcardReference, aCtWildcardReference);
		this.other = aCtWildcardReference;
	}

	@java.lang.Override
	public <T> void visitCtIntersectionTypeReference(final CtIntersectionTypeReference<T> reference) {
		CtIntersectionTypeReference<T> aCtIntersectionTypeReference = this.factory.Core().createIntersectionTypeReference();
		aCtIntersectionTypeReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtIntersectionTypeReference);
		aCtIntersectionTypeReference.setPackage(this.cloneHelper.clone(reference.getPackage()));
		aCtIntersectionTypeReference.setDeclaringType(this.cloneHelper.clone(reference.getDeclaringType()));
		aCtIntersectionTypeReference.setActualTypeArguments(this.cloneHelper.clone(reference.getActualTypeArguments()));
		aCtIntersectionTypeReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		aCtIntersectionTypeReference.setBounds(this.cloneHelper.clone(reference.getBounds()));
		this.cloneHelper.tailor(reference, aCtIntersectionTypeReference);
		this.other = aCtIntersectionTypeReference;
	}

	public <T> void visitCtTypeReference(final CtTypeReference<T> reference) {
		CtTypeReference<T> aCtTypeReference = this.factory.Core().createTypeReference();
		aCtTypeReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtTypeReference);
		aCtTypeReference.setPackage(this.cloneHelper.clone(reference.getPackage()));
		aCtTypeReference.setDeclaringType(this.cloneHelper.clone(reference.getDeclaringType()));
		aCtTypeReference.setActualTypeArguments(this.cloneHelper.clone(reference.getActualTypeArguments()));
		aCtTypeReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		aCtTypeReference.setComments(this.cloneHelper.clone(reference.getComments()));
		this.cloneHelper.tailor(reference, aCtTypeReference);
		this.other = aCtTypeReference;
	}

	@java.lang.Override
	public <T> void visitCtTypeAccess(final CtTypeAccess<T> typeAccess) {
		CtTypeAccess<T> aCtTypeAccess = this.factory.Core().createTypeAccess();
		aCtTypeAccess.setPosition(typeAccess.getPosition());
		this.builder.copy(typeAccess, aCtTypeAccess);
		aCtTypeAccess.setAnnotations(this.cloneHelper.clone(typeAccess.getAnnotations()));
		aCtTypeAccess.setTypeCasts(this.cloneHelper.clone(typeAccess.getTypeCasts()));
		aCtTypeAccess.setAccessedType(this.cloneHelper.clone(typeAccess.getAccessedType()));
		aCtTypeAccess.setComments(this.cloneHelper.clone(typeAccess.getComments()));
		this.cloneHelper.tailor(typeAccess, aCtTypeAccess);
		this.other = aCtTypeAccess;
	}

	public <T> void visitCtUnaryOperator(final CtUnaryOperator<T> operator) {
		CtUnaryOperator<T> aCtUnaryOperator = this.factory.Core().createUnaryOperator();
		aCtUnaryOperator.setPosition(operator.getPosition());
		this.builder.copy(operator, aCtUnaryOperator);
		aCtUnaryOperator.setAnnotations(this.cloneHelper.clone(operator.getAnnotations()));
		aCtUnaryOperator.setType(this.cloneHelper.clone(operator.getType()));
		aCtUnaryOperator.setTypeCasts(this.cloneHelper.clone(operator.getTypeCasts()));
		aCtUnaryOperator.setOperand(this.cloneHelper.clone(operator.getOperand()));
		aCtUnaryOperator.setComments(this.cloneHelper.clone(operator.getComments()));
		this.cloneHelper.tailor(operator, aCtUnaryOperator);
		this.other = aCtUnaryOperator;
	}

	@java.lang.Override
	public <T> void visitCtVariableRead(final CtVariableRead<T> variableRead) {
		CtVariableRead<T> aCtVariableRead = this.factory.Core().createVariableRead();
		aCtVariableRead.setPosition(variableRead.getPosition());
		this.builder.copy(variableRead, aCtVariableRead);
		aCtVariableRead.setAnnotations(this.cloneHelper.clone(variableRead.getAnnotations()));
		aCtVariableRead.setTypeCasts(this.cloneHelper.clone(variableRead.getTypeCasts()));
		aCtVariableRead.setVariable(this.cloneHelper.clone(variableRead.getVariable()));
		aCtVariableRead.setComments(this.cloneHelper.clone(variableRead.getComments()));
		this.cloneHelper.tailor(variableRead, aCtVariableRead);
		this.other = aCtVariableRead;
	}

	@java.lang.Override
	public <T> void visitCtVariableWrite(final CtVariableWrite<T> variableWrite) {
		CtVariableWrite<T> aCtVariableWrite = this.factory.Core().createVariableWrite();
		aCtVariableWrite.setPosition(variableWrite.getPosition());
		this.builder.copy(variableWrite, aCtVariableWrite);
		aCtVariableWrite.setAnnotations(this.cloneHelper.clone(variableWrite.getAnnotations()));
		aCtVariableWrite.setTypeCasts(this.cloneHelper.clone(variableWrite.getTypeCasts()));
		aCtVariableWrite.setVariable(this.cloneHelper.clone(variableWrite.getVariable()));
		aCtVariableWrite.setComments(this.cloneHelper.clone(variableWrite.getComments()));
		this.cloneHelper.tailor(variableWrite, aCtVariableWrite);
		this.other = aCtVariableWrite;
	}

	public void visitCtWhile(final CtWhile whileLoop) {
		CtWhile aCtWhile = this.factory.Core().createWhile();
		aCtWhile.setPosition(whileLoop.getPosition());
		this.builder.copy(whileLoop, aCtWhile);
		aCtWhile.setAnnotations(this.cloneHelper.clone(whileLoop.getAnnotations()));
		aCtWhile.setLoopingExpression(this.cloneHelper.clone(whileLoop.getLoopingExpression()));
		aCtWhile.setBody(this.cloneHelper.clone(whileLoop.getBody()));
		aCtWhile.setComments(this.cloneHelper.clone(whileLoop.getComments()));
		this.cloneHelper.tailor(whileLoop, aCtWhile);
		this.other = aCtWhile;
	}

	public <T> void visitCtCodeSnippetExpression(final CtCodeSnippetExpression<T> expression) {
		CtCodeSnippetExpression<T> aCtCodeSnippetExpression = this.factory.Core().createCodeSnippetExpression();
		aCtCodeSnippetExpression.setPosition(expression.getPosition());
		this.builder.copy(expression, aCtCodeSnippetExpression);
		aCtCodeSnippetExpression.setType(this.cloneHelper.clone(expression.getType()));
		aCtCodeSnippetExpression.setComments(this.cloneHelper.clone(expression.getComments()));
		aCtCodeSnippetExpression.setAnnotations(this.cloneHelper.clone(expression.getAnnotations()));
		aCtCodeSnippetExpression.setTypeCasts(this.cloneHelper.clone(expression.getTypeCasts()));
		this.cloneHelper.tailor(expression, aCtCodeSnippetExpression);
		this.other = aCtCodeSnippetExpression;
	}

	public void visitCtCodeSnippetStatement(final CtCodeSnippetStatement statement) {
		CtCodeSnippetStatement aCtCodeSnippetStatement = this.factory.Core().createCodeSnippetStatement();
		aCtCodeSnippetStatement.setPosition(statement.getPosition());
		this.builder.copy(statement, aCtCodeSnippetStatement);
		aCtCodeSnippetStatement.setComments(this.cloneHelper.clone(statement.getComments()));
		aCtCodeSnippetStatement.setAnnotations(this.cloneHelper.clone(statement.getAnnotations()));
		this.cloneHelper.tailor(statement, aCtCodeSnippetStatement);
		this.other = aCtCodeSnippetStatement;
	}

	public <T> void visitCtUnboundVariableReference(final CtUnboundVariableReference<T> reference) {
		CtUnboundVariableReference<T> aCtUnboundVariableReference = this.factory.Core().createUnboundVariableReference();
		aCtUnboundVariableReference.setPosition(reference.getPosition());
		this.builder.copy(reference, aCtUnboundVariableReference);
		aCtUnboundVariableReference.setType(this.cloneHelper.clone(reference.getType()));
		this.cloneHelper.tailor(reference, aCtUnboundVariableReference);
		this.other = aCtUnboundVariableReference;
	}

	@java.lang.Override
	public <T> void visitCtFieldRead(final CtFieldRead<T> fieldRead) {
		CtFieldRead<T> aCtFieldRead = this.factory.Core().createFieldRead();
		aCtFieldRead.setPosition(fieldRead.getPosition());
		this.builder.copy(fieldRead, aCtFieldRead);
		aCtFieldRead.setAnnotations(this.cloneHelper.clone(fieldRead.getAnnotations()));
		aCtFieldRead.setTypeCasts(this.cloneHelper.clone(fieldRead.getTypeCasts()));
		aCtFieldRead.setTarget(this.cloneHelper.clone(fieldRead.getTarget()));
		aCtFieldRead.setVariable(this.cloneHelper.clone(fieldRead.getVariable()));
		aCtFieldRead.setComments(this.cloneHelper.clone(fieldRead.getComments()));
		this.cloneHelper.tailor(fieldRead, aCtFieldRead);
		this.other = aCtFieldRead;
	}

	@java.lang.Override
	public <T> void visitCtFieldWrite(final CtFieldWrite<T> fieldWrite) {
		CtFieldWrite<T> aCtFieldWrite = this.factory.Core().createFieldWrite();
		aCtFieldWrite.setPosition(fieldWrite.getPosition());
		this.builder.copy(fieldWrite, aCtFieldWrite);
		aCtFieldWrite.setAnnotations(this.cloneHelper.clone(fieldWrite.getAnnotations()));
		aCtFieldWrite.setTypeCasts(this.cloneHelper.clone(fieldWrite.getTypeCasts()));
		aCtFieldWrite.setTarget(this.cloneHelper.clone(fieldWrite.getTarget()));
		aCtFieldWrite.setVariable(this.cloneHelper.clone(fieldWrite.getVariable()));
		aCtFieldWrite.setComments(this.cloneHelper.clone(fieldWrite.getComments()));
		this.cloneHelper.tailor(fieldWrite, aCtFieldWrite);
		this.other = aCtFieldWrite;
	}

	@java.lang.Override
	public <T> void visitCtSuperAccess(final CtSuperAccess<T> f) {
		CtSuperAccess<T> aCtSuperAccess = this.factory.Core().createSuperAccess();
		aCtSuperAccess.setPosition(f.getPosition());
		this.builder.copy(f, aCtSuperAccess);
		aCtSuperAccess.setComments(this.cloneHelper.clone(f.getComments()));
		aCtSuperAccess.setAnnotations(this.cloneHelper.clone(f.getAnnotations()));
		aCtSuperAccess.setTypeCasts(this.cloneHelper.clone(f.getTypeCasts()));
		aCtSuperAccess.setTarget(this.cloneHelper.clone(f.getTarget()));
		aCtSuperAccess.setVariable(this.cloneHelper.clone(f.getVariable()));
		this.cloneHelper.tailor(f, aCtSuperAccess);
		this.other = aCtSuperAccess;
	}

	@java.lang.Override
	public void visitCtComment(final CtComment comment) {
		CtComment aCtComment = this.factory.Core().createComment();
		aCtComment.setPosition(comment.getPosition());
		this.builder.copy(comment, aCtComment);
		aCtComment.setComments(this.cloneHelper.clone(comment.getComments()));
		aCtComment.setAnnotations(this.cloneHelper.clone(comment.getAnnotations()));
		this.cloneHelper.tailor(comment, aCtComment);
		this.other = aCtComment;
	}

	@java.lang.Override
	public void visitCtJavaDoc(final CtJavaDoc javaDoc) {
		CtJavaDoc aCtJavaDoc = this.factory.Core().createJavaDoc();
		aCtJavaDoc.setPosition(javaDoc.getPosition());
		this.builder.copy(javaDoc, aCtJavaDoc);
		aCtJavaDoc.setComments(this.cloneHelper.clone(javaDoc.getComments()));
		aCtJavaDoc.setAnnotations(this.cloneHelper.clone(javaDoc.getAnnotations()));
		aCtJavaDoc.setTags(this.cloneHelper.clone(javaDoc.getTags()));
		this.cloneHelper.tailor(javaDoc, aCtJavaDoc);
		this.other = aCtJavaDoc;
	}

	@java.lang.Override
	public void visitCtJavaDocTag(final CtJavaDocTag docTag) {
		CtJavaDocTag aCtJavaDocTag = this.factory.Core().createJavaDocTag();
		aCtJavaDocTag.setPosition(docTag.getPosition());
		this.builder.copy(docTag, aCtJavaDocTag);
		aCtJavaDocTag.setComments(this.cloneHelper.clone(docTag.getComments()));
		aCtJavaDocTag.setAnnotations(this.cloneHelper.clone(docTag.getAnnotations()));
		this.cloneHelper.tailor(docTag, aCtJavaDocTag);
		this.other = aCtJavaDocTag;
	}

	@java.lang.Override
	public void visitCtImport(final CtImport ctImport) {
		CtImport aCtImport = this.factory.Core().createImport();
		aCtImport.setPosition(ctImport.getPosition());
		this.builder.copy(ctImport, aCtImport);
		aCtImport.setReference(this.cloneHelper.clone(ctImport.getReference()));
		aCtImport.setAnnotations(this.cloneHelper.clone(ctImport.getAnnotations()));
		aCtImport.setComments(this.cloneHelper.clone(ctImport.getComments()));
		this.cloneHelper.tailor(ctImport, aCtImport);
		this.other = aCtImport;
	}

	@java.lang.Override
	public void visitCtModule(CtModule module) {
		CtModule aCtModule = this.factory.Core().createModule();
		aCtModule.setPosition(module.getPosition());
		this.builder.copy(module, aCtModule);
		aCtModule.setComments(this.cloneHelper.clone(module.getComments()));
		aCtModule.setAnnotations(this.cloneHelper.clone(module.getAnnotations()));
		aCtModule.setModuleDirectives(this.cloneHelper.clone(module.getModuleDirectives()));
		aCtModule.setRootPackage(this.cloneHelper.clone(module.getRootPackage()));
		this.cloneHelper.tailor(module, aCtModule);
		this.other = aCtModule;
	}

	@java.lang.Override
	public void visitCtModuleReference(CtModuleReference moduleReference) {
		CtModuleReference aCtModuleReference = this.factory.Core().createModuleReference();
		aCtModuleReference.setPosition(moduleReference.getPosition());
		this.builder.copy(moduleReference, aCtModuleReference);
		aCtModuleReference.setAnnotations(this.cloneHelper.clone(moduleReference.getAnnotations()));
		this.cloneHelper.tailor(moduleReference, aCtModuleReference);
		this.other = aCtModuleReference;
	}

	@java.lang.Override
	public void visitCtPackageExport(CtPackageExport moduleExport) {
		CtPackageExport aCtPackageExport = this.factory.Core().createPackageExport();
		aCtPackageExport.setPosition(moduleExport.getPosition());
		this.builder.copy(moduleExport, aCtPackageExport);
		aCtPackageExport.setComments(this.cloneHelper.clone(moduleExport.getComments()));
		aCtPackageExport.setPackageReference(this.cloneHelper.clone(moduleExport.getPackageReference()));
		aCtPackageExport.setTargetExport(this.cloneHelper.clone(moduleExport.getTargetExport()));
		aCtPackageExport.setAnnotations(this.cloneHelper.clone(moduleExport.getAnnotations()));
		this.cloneHelper.tailor(moduleExport, aCtPackageExport);
		this.other = aCtPackageExport;
	}

	@java.lang.Override
	public void visitCtModuleRequirement(CtModuleRequirement moduleRequirement) {
		CtModuleRequirement aCtModuleRequirement = this.factory.Core().createModuleRequirement();
		aCtModuleRequirement.setPosition(moduleRequirement.getPosition());
		this.builder.copy(moduleRequirement, aCtModuleRequirement);
		aCtModuleRequirement.setComments(this.cloneHelper.clone(moduleRequirement.getComments()));
		aCtModuleRequirement.setModuleReference(this.cloneHelper.clone(moduleRequirement.getModuleReference()));
		aCtModuleRequirement.setAnnotations(this.cloneHelper.clone(moduleRequirement.getAnnotations()));
		this.cloneHelper.tailor(moduleRequirement, aCtModuleRequirement);
		this.other = aCtModuleRequirement;
	}

	@java.lang.Override
	public void visitCtProvidedService(CtProvidedService moduleProvidedService) {
		CtProvidedService aCtProvidedService = this.factory.Core().createProvidedService();
		aCtProvidedService.setPosition(moduleProvidedService.getPosition());
		this.builder.copy(moduleProvidedService, aCtProvidedService);
		aCtProvidedService.setComments(this.cloneHelper.clone(moduleProvidedService.getComments()));
		aCtProvidedService.setServiceType(this.cloneHelper.clone(moduleProvidedService.getServiceType()));
		aCtProvidedService
				.setImplementationTypes(this.cloneHelper.clone(moduleProvidedService.getImplementationTypes()));
		aCtProvidedService.setAnnotations(this.cloneHelper.clone(moduleProvidedService.getAnnotations()));
		this.cloneHelper.tailor(moduleProvidedService, aCtProvidedService);
		this.other = aCtProvidedService;
	}

	@java.lang.Override
	public void visitCtUsedService(CtUsedService usedService) {
		CtUsedService aCtUsedService = this.factory.Core().createUsedService();
		aCtUsedService.setPosition(usedService.getPosition());
		this.builder.copy(usedService, aCtUsedService);
		aCtUsedService.setComments(this.cloneHelper.clone(usedService.getComments()));
		aCtUsedService.setServiceType(this.cloneHelper.clone(usedService.getServiceType()));
		aCtUsedService.setAnnotations(this.cloneHelper.clone(usedService.getAnnotations()));
		this.cloneHelper.tailor(usedService, aCtUsedService);
		this.other = aCtUsedService;
	}

	@java.lang.Override
	public void visitCtCompilationUnit(CtCompilationUnit compilationUnit) {
		CtCompilationUnit aCtCompilationUnit = this.factory.Core().createCompilationUnit();
		aCtCompilationUnit.setPosition(compilationUnit.getPosition());
		this.builder.copy(compilationUnit, aCtCompilationUnit);
		aCtCompilationUnit.setComments(this.cloneHelper.clone(compilationUnit.getComments()));
		aCtCompilationUnit.setAnnotations(this.cloneHelper.clone(compilationUnit.getAnnotations()));
		aCtCompilationUnit.setPackageDeclaration(this.cloneHelper.clone(compilationUnit.getPackageDeclaration()));
		aCtCompilationUnit.setImports(this.cloneHelper.clone(compilationUnit.getImports()));
		aCtCompilationUnit
				.setDeclaredModuleReference(this.cloneHelper.clone(compilationUnit.getDeclaredModuleReference()));
		aCtCompilationUnit
				.setDeclaredTypeReferences(this.cloneHelper.clone(compilationUnit.getDeclaredTypeReferences()));
		this.cloneHelper.tailor(compilationUnit, aCtCompilationUnit);
		this.other = aCtCompilationUnit;
	}

	@java.lang.Override
	public void visitCtPackageDeclaration(CtPackageDeclaration packageDeclaration) {
		CtPackageDeclaration aCtPackageDeclaration = this.factory.Core().createPackageDeclaration();
		aCtPackageDeclaration.setPosition(packageDeclaration.getPosition());
		this.builder.copy(packageDeclaration, aCtPackageDeclaration);
		aCtPackageDeclaration.setComments(this.cloneHelper.clone(packageDeclaration.getComments()));
		aCtPackageDeclaration.setAnnotations(this.cloneHelper.clone(packageDeclaration.getAnnotations()));
		aCtPackageDeclaration.setReference(this.cloneHelper.clone(packageDeclaration.getReference()));
		this.cloneHelper.tailor(packageDeclaration, aCtPackageDeclaration);
		this.other = aCtPackageDeclaration;
	}

	@java.lang.Override
	public void visitCtTypeMemberWildcardImportReference(CtTypeMemberWildcardImportReference wildcardReference) {
		CtTypeMemberWildcardImportReference aCtTypeMemberWildcardImportReference = this.factory.Core().createTypeMemberWildcardImportReference();
		aCtTypeMemberWildcardImportReference.setPosition(wildcardReference.getPosition());
		this.builder.copy(wildcardReference, aCtTypeMemberWildcardImportReference);
		aCtTypeMemberWildcardImportReference
				.setTypeReference(this.cloneHelper.clone(wildcardReference.getTypeReference()));
		this.cloneHelper.tailor(wildcardReference, aCtTypeMemberWildcardImportReference);
		this.other = aCtTypeMemberWildcardImportReference;
	}
}