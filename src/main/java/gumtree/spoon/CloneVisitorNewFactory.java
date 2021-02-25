package gumtree.spoon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.ITree;

import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.experimental.CtUnresolvedImport;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.*;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.BodyHolderSourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.CtScanner;
import spoon.support.reflect.CtExtendedModifier;
import spoon.support.reflect.cu.position.BodyHolderSourcePositionImpl;
import spoon.support.reflect.cu.position.CompoundSourcePositionImpl;
import spoon.support.reflect.cu.position.DeclarationSourcePositionImpl;
import spoon.support.reflect.cu.position.PartialSourcePositionImpl;
import spoon.support.reflect.cu.position.SourcePositionImpl;
import spoon.support.visitor.clone.CloneBuilder;
import spoon.support.visitor.equals.CloneHelper;

/**
 * should be similar to NodeCreator mostly used for insertion ?
 */
public class CloneVisitorNewFactory extends CtScanner {
	protected final CloneHelper cloneHelper;

	protected final CloneBuilder builder = new CloneBuilder();

	protected final Factory factory;

	protected CtElement other;

	public CloneVisitorNewFactory(CloneHelper cloneHelper, Factory factory) {
		this.cloneHelper = cloneHelper;
		this.factory = factory;
	}

	public <T extends CtElement> T getClone() {
		return ((T) (other));
	}

	public SourcePosition clonePosition(SourcePosition pos) {
		if (pos instanceof PartialSourcePositionImpl){
		} else if (pos instanceof NoSourcePosition)
			return null;
		CompilationUnit newcu = this.factory.CompilationUnit().getMap().get(pos.getFile().getPath());
		if (newcu == null) {
			newcu = this.cloneHelper.clone(pos.getCompilationUnit());
			this.factory.CompilationUnit().getMap().put(pos.getFile().getPath(), newcu);
		}
		return clonePositionAux(pos, newcu);
	}

	public SourcePosition clonePositionAux(SourcePosition pos, CompilationUnit newcu) {
		SourcePosition newpos;
		if (pos instanceof BodyHolderSourcePositionImpl) {
			BodyHolderSourcePositionImpl casted = (BodyHolderSourcePositionImpl) pos;
			newpos = this.factory.Core().createBodyHolderSourcePosition(newcu, casted.getNameStart(),
					casted.getNameEnd(), casted.getModifierSourceStart(), casted.getModifierSourceEnd(),
					casted.getSourceStart(), casted.getSourceEnd(), casted.getBodyStart(), casted.getBodyEnd(),
					newcu.getLineSeparatorPositions());
		} else if (pos instanceof DeclarationSourcePositionImpl) {
			DeclarationSourcePositionImpl casted = (DeclarationSourcePositionImpl) pos;
			newpos = this.factory.Core().createDeclarationSourcePosition(newcu, casted.getNameStart(),
					casted.getNameEnd(), casted.getModifierSourceStart(), casted.getModifierSourceEnd(),
					casted.getSourceStart(), casted.getSourceEnd(), newcu.getLineSeparatorPositions());
		} else if (pos instanceof CompoundSourcePositionImpl) {
			CompoundSourcePositionImpl casted = (CompoundSourcePositionImpl) pos;
			newpos = this.factory.Core().createCompoundSourcePosition(newcu, casted.getNameStart(), casted.getNameEnd(),
					casted.getSourceStart(), casted.getSourceEnd(), newcu.getLineSeparatorPositions());
		} else if (pos instanceof PartialSourcePositionImpl) {
			newpos = this.factory.Core().createPartialSourcePosition(newcu);
		} else if (pos instanceof NoSourcePosition) {
			newpos = null;
		} else if (pos instanceof SourcePositionImpl) {
			SourcePositionImpl casted = (SourcePositionImpl) pos;
			newpos = this.factory.Core().createSourcePosition(newcu, casted.getSourceStart(), casted.getSourceEnd(),
					newcu.getLineSeparatorPositions());
		} else {
			throw new RuntimeException(pos.getClass().toString());
		}
		return newpos;
	}

	public <A extends java.lang.annotation.Annotation> void visitCtAnnotation(final CtAnnotation<A> annotation) {
		CtAnnotation<A> aCtAnnotation = this.factory.Core().createAnnotation();
		this.builder.copy(annotation, aCtAnnotation);
		aCtAnnotation.setPosition(clonePosition(annotation.getPosition()));
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
		this.builder.copy(annotationType, aCtAnnotationType);
		aCtAnnotationType.setPosition(clonePosition(annotationType.getPosition()));
		aCtAnnotationType.setAnnotations(this.cloneHelper.clone(annotationType.getAnnotations()));
		aCtAnnotationType.setTypeMembers(this.cloneHelper.clone(annotationType.getTypeMembers()));
		aCtAnnotationType.setComments(this.cloneHelper.clone(annotationType.getComments()));
		this.cloneHelper.tailor(annotationType, aCtAnnotationType);
		this.other = aCtAnnotationType;
	}

	public void visitCtAnonymousExecutable(final CtAnonymousExecutable anonymousExec) {
		CtAnonymousExecutable aCtAnonymousExecutable = this.factory.Core().createAnonymousExecutable();
		this.builder.copy(anonymousExec, aCtAnonymousExecutable);
		aCtAnonymousExecutable.setPosition(clonePosition(anonymousExec.getPosition()));
		aCtAnonymousExecutable.setExtendedModifiers(cloneModifiers(anonymousExec,aCtAnonymousExecutable));
		aCtAnonymousExecutable.setAnnotations(this.cloneHelper.clone(anonymousExec.getAnnotations()));
		aCtAnonymousExecutable.setBody(this.cloneHelper.clone(anonymousExec.getBody()));
		aCtAnonymousExecutable.setComments(this.cloneHelper.clone(anonymousExec.getComments()));
		this.cloneHelper.tailor(anonymousExec, aCtAnonymousExecutable);
		this.other = aCtAnonymousExecutable;
	}

	@java.lang.Override
	public <T> void visitCtArrayRead(final CtArrayRead<T> arrayRead) {
		CtArrayRead<T> aCtArrayRead = this.factory.Core().createArrayRead();
		this.builder.copy(arrayRead, aCtArrayRead);
		aCtArrayRead.setPosition(clonePosition(arrayRead.getPosition()));
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
		this.builder.copy(arrayWrite, aCtArrayWrite);
		aCtArrayWrite.setPosition(clonePosition(arrayWrite.getPosition()));
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
		this.builder.copy(reference, aCtArrayTypeReference);
		aCtArrayTypeReference.setPosition(clonePosition(reference.getPosition()));
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
		this.builder.copy(asserted, aCtAssert);
		aCtAssert.setPosition(clonePosition(asserted.getPosition()));
		aCtAssert.setAnnotations(this.cloneHelper.clone(asserted.getAnnotations()));
		aCtAssert.setAssertExpression(this.cloneHelper.clone(asserted.getAssertExpression()));
		aCtAssert.setExpression(this.cloneHelper.clone(asserted.getExpression()));
		aCtAssert.setComments(this.cloneHelper.clone(asserted.getComments()));
		this.cloneHelper.tailor(asserted, aCtAssert);
		this.other = aCtAssert;
	}

	public <T, A extends T> void visitCtAssignment(final CtAssignment<T, A> assignement) {
		CtAssignment<T, A> aCtAssignment = this.factory.Core().createAssignment();
		this.builder.copy(assignement, aCtAssignment);
		aCtAssignment.setPosition(clonePosition(assignement.getPosition()));
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
		this.builder.copy(operator, aCtBinaryOperator);
		aCtBinaryOperator.setPosition(clonePosition(operator.getPosition()));
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
		this.builder.copy(block, aCtBlock);
		aCtBlock.setPosition(clonePosition(block.getPosition()));
		aCtBlock.setAnnotations(this.cloneHelper.clone(block.getAnnotations()));
		aCtBlock.setStatements(this.cloneHelper.clone(block.getStatements()));
		aCtBlock.setComments(this.cloneHelper.clone(block.getComments()));
		this.cloneHelper.tailor(block, aCtBlock);
		this.other = aCtBlock;
	}

	public void visitCtBreak(final CtBreak breakStatement) {
		CtBreak aCtBreak = this.factory.Core().createBreak();
		this.builder.copy(breakStatement, aCtBreak);
		aCtBreak.setPosition(clonePosition(breakStatement.getPosition()));
		aCtBreak.setAnnotations(this.cloneHelper.clone(breakStatement.getAnnotations()));
		aCtBreak.setComments(this.cloneHelper.clone(breakStatement.getComments()));
		this.cloneHelper.tailor(breakStatement, aCtBreak);
		this.other = aCtBreak;
	}

	public <S> void visitCtCase(final CtCase<S> caseStatement) {
		CtCase<S> aCtCase = this.factory.Core().createCase();
		this.builder.copy(caseStatement, aCtCase);
		aCtCase.setPosition(clonePosition(caseStatement.getPosition()));
		aCtCase.setAnnotations(this.cloneHelper.clone(caseStatement.getAnnotations()));
		aCtCase.setCaseExpression(this.cloneHelper.clone(caseStatement.getCaseExpression()));
		aCtCase.setStatements(this.cloneHelper.clone(caseStatement.getStatements()));
		aCtCase.setComments(this.cloneHelper.clone(caseStatement.getComments()));
		this.cloneHelper.tailor(caseStatement, aCtCase);
		this.other = aCtCase;
	}

	public void visitCtCatch(final CtCatch catchBlock) {
		CtCatch aCtCatch = this.factory.Core().createCatch();
		this.builder.copy(catchBlock, aCtCatch);
		aCtCatch.setPosition(clonePosition(catchBlock.getPosition()));
		aCtCatch.setAnnotations(this.cloneHelper.clone(catchBlock.getAnnotations()));
		aCtCatch.setParameter(this.cloneHelper.clone(catchBlock.getParameter()));
		aCtCatch.setBody(this.cloneHelper.clone(catchBlock.getBody()));
		aCtCatch.setComments(this.cloneHelper.clone(catchBlock.getComments()));
		this.cloneHelper.tailor(catchBlock, aCtCatch);
		this.other = aCtCatch;
	}

	public <T> void visitCtClass(final CtClass<T> ctClass) {
		CtClass<T> aCtClass = this.factory.Core().createClass();
		this.builder.copy(ctClass, aCtClass);
		aCtClass.setPosition(clonePosition(ctClass.getPosition()));
		aCtClass.setExtendedModifiers(cloneModifiers(ctClass,aCtClass));
		aCtClass.setAnnotations(this.cloneHelper.clone(ctClass.getAnnotations()));
		aCtClass.setSuperclass(this.cloneHelper.clone(ctClass.getSuperclass()));
		aCtClass.setSuperInterfaces(this.cloneHelper.clone(ctClass.getSuperInterfaces()));
		aCtClass.setFormalCtTypeParameters(this.cloneHelper.clone(ctClass.getFormalCtTypeParameters()));
		aCtClass.setTypeMembers(this.cloneHelper.clone(ctClass.getTypeMembers()));
		aCtClass.setComments(this.cloneHelper.clone(ctClass.getComments()));
		if (aCtClass.isTopLevel()) {
			aCtClass.getPosition().getCompilationUnit().addDeclaredType(aCtClass);
		}
		this.cloneHelper.tailor(ctClass, aCtClass);
		this.other = aCtClass;
	}

	@java.lang.Override
	public void visitCtTypeParameter(CtTypeParameter typeParameter) {
		CtTypeParameter aCtTypeParameter = this.factory.Core().createTypeParameter();
		this.builder.copy(typeParameter, aCtTypeParameter);
		aCtTypeParameter.setPosition(clonePosition(typeParameter.getPosition()));
		aCtTypeParameter.setAnnotations(this.cloneHelper.clone(typeParameter.getAnnotations()));
		aCtTypeParameter.setSuperclass(this.cloneHelper.clone(typeParameter.getSuperclass()));
		aCtTypeParameter.setComments(this.cloneHelper.clone(typeParameter.getComments()));
		this.cloneHelper.tailor(typeParameter, aCtTypeParameter);
		this.other = aCtTypeParameter;
	}

	public <T> void visitCtConditional(final CtConditional<T> conditional) {
		CtConditional<T> aCtConditional = this.factory.Core().createConditional();
		this.builder.copy(conditional, aCtConditional);
		aCtConditional.setPosition(clonePosition(conditional.getPosition()));
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
		this.builder.copy(c, aCtConstructor);
		aCtConstructor.setPosition(clonePosition(c.getPosition()));
		aCtConstructor.setExtendedModifiers(cloneModifiers(c,aCtConstructor));
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
		this.builder.copy(continueStatement, aCtContinue);
		aCtContinue.setPosition(clonePosition(continueStatement.getPosition()));
		aCtContinue.setAnnotations(this.cloneHelper.clone(continueStatement.getAnnotations()));
		aCtContinue.setComments(this.cloneHelper.clone(continueStatement.getComments()));
		this.cloneHelper.tailor(continueStatement, aCtContinue);
		this.other = aCtContinue;
	}

	public void visitCtDo(final CtDo doLoop) {
		CtDo aCtDo = this.factory.Core().createDo();
		this.builder.copy(doLoop, aCtDo);
		aCtDo.setPosition(clonePosition(doLoop.getPosition()));
		aCtDo.setAnnotations(this.cloneHelper.clone(doLoop.getAnnotations()));
		aCtDo.setLoopingExpression(this.cloneHelper.clone(doLoop.getLoopingExpression()));
		aCtDo.setBody(this.cloneHelper.clone(doLoop.getBody()));
		aCtDo.setComments(this.cloneHelper.clone(doLoop.getComments()));
		this.cloneHelper.tailor(doLoop, aCtDo);
		this.other = aCtDo;
	}

	public <T extends java.lang.Enum<?>> void visitCtEnum(final CtEnum<T> ctEnum) {
		CtEnum<T> aCtEnum = this.factory.Core().createEnum();
		this.builder.copy(ctEnum, aCtEnum);
		aCtEnum.setPosition(clonePosition(ctEnum.getPosition()));
		aCtEnum.setAnnotations(this.cloneHelper.clone(ctEnum.getAnnotations()));
		aCtEnum.setSuperInterfaces(this.cloneHelper.clone(ctEnum.getSuperInterfaces()));
		aCtEnum.setTypeMembers(this.cloneHelper.clone(ctEnum.getTypeMembers()));
		aCtEnum.setEnumValues(this.cloneHelper.clone(ctEnum.getEnumValues()));
		aCtEnum.setComments(this.cloneHelper.clone(ctEnum.getComments()));
		if (aCtEnum.isTopLevel()) {
			aCtEnum.getPosition().getCompilationUnit().addDeclaredType(aCtEnum);
		}
		this.cloneHelper.tailor(ctEnum, aCtEnum);
		this.other = aCtEnum;
	}

	public <T> void visitCtExecutableReference(final CtExecutableReference<T> reference) {
		CtExecutableReference<T> aCtExecutableReference = this.factory.Core().createExecutableReference();
		this.builder.copy(reference, aCtExecutableReference);
		aCtExecutableReference.setPosition(clonePosition(reference.getPosition()));
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
		this.builder.copy(f, aCtField);
		aCtField.setPosition(clonePosition(f.getPosition()));
		aCtField.setExtendedModifiers(cloneModifiers(f,aCtField));
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
		this.builder.copy(enumValue, aCtEnumValue);
		aCtEnumValue.setPosition(clonePosition(enumValue.getPosition()));
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
		this.builder.copy(thisAccess, aCtThisAccess);
		aCtThisAccess.setPosition(clonePosition(thisAccess.getPosition()));
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
		this.builder.copy(annotationFieldAccess, aCtAnnotationFieldAccess);
		aCtAnnotationFieldAccess.setPosition(clonePosition(annotationFieldAccess.getPosition()));
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
		this.builder.copy(reference, aCtFieldReference);
		aCtFieldReference.setPosition(clonePosition(reference.getPosition()));
		aCtFieldReference.setDeclaringType(this.cloneHelper.clone(reference.getDeclaringType()));
		aCtFieldReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtFieldReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtFieldReference);
		this.other = aCtFieldReference;
	}

	public void visitCtFor(final CtFor forLoop) {
		CtFor aCtFor = this.factory.Core().createFor();
		this.builder.copy(forLoop, aCtFor);
		aCtFor.setPosition(clonePosition(forLoop.getPosition()));
		aCtFor.setAnnotations(this.cloneHelper.clone(forLoop.getAnnotations()));
		aCtFor.setForInit(this.cloneHelper.clone(forLoop.getForInit()));
		aCtFor.setExpression(this.cloneHelper.clone(forLoop.getExpression()));
		aCtFor.setForUpdate(this.cloneHelper.clone(forLoop.getForUpdate()));
		aCtFor.setBody(this.cloneHelper.clone(forLoop.getBody()));
		if (forLoop.getBody() != null) {
			aCtFor.getBody().setImplicit(forLoop.getBody().isImplicit());
		}
		aCtFor.setComments(this.cloneHelper.clone(forLoop.getComments()));
		this.cloneHelper.tailor(forLoop, aCtFor);
		this.other = aCtFor;
	}

	public void visitCtForEach(final CtForEach foreach) {
		CtForEach aCtForEach = this.factory.Core().createForEach();
		this.builder.copy(foreach, aCtForEach);
		aCtForEach.setPosition(clonePosition(foreach.getPosition()));
		aCtForEach.setAnnotations(this.cloneHelper.clone(foreach.getAnnotations()));
		aCtForEach.setVariable(this.cloneHelper.clone(foreach.getVariable()));
		aCtForEach.setExpression(this.cloneHelper.clone(foreach.getExpression()));
		aCtForEach.setBody(this.cloneHelper.clone(foreach.getBody()));
		if (foreach.getBody() != null) {
			aCtForEach.getBody().setImplicit(foreach.getBody().isImplicit());
		}
		aCtForEach.setComments(this.cloneHelper.clone(foreach.getComments()));
		this.cloneHelper.tailor(foreach, aCtForEach);
		this.other = aCtForEach;
	}

	public void visitCtIf(final CtIf ifElement) {
		CtIf aCtIf = this.factory.Core().createIf();
		this.builder.copy(ifElement, aCtIf);
		aCtIf.setPosition(clonePosition(ifElement.getPosition()));
		aCtIf.setAnnotations(this.cloneHelper.clone(ifElement.getAnnotations()));
		aCtIf.setCondition(this.cloneHelper.clone(ifElement.getCondition()));
		aCtIf.setThenStatement(this.cloneHelper.clone(((CtStatement) (ifElement.getThenStatement()))));
		if (ifElement.getThenStatement() != null) {
			aCtIf.getThenStatement().setImplicit(ifElement.getThenStatement().isImplicit());
		}
		aCtIf.setElseStatement(this.cloneHelper.clone(((CtStatement) (ifElement.getElseStatement()))));
		if (ifElement.getElseStatement() != null) {
			aCtIf.getElseStatement().setImplicit(ifElement.getElseStatement().isImplicit());
		}
		aCtIf.setComments(this.cloneHelper.clone(ifElement.getComments()));
		this.cloneHelper.tailor(ifElement, aCtIf);
		this.other = aCtIf;
	}

	public <T> void visitCtInterface(final CtInterface<T> intrface) {
		CtInterface<T> aCtInterface = this.factory.Core().createInterface();
		this.builder.copy(intrface, aCtInterface);
		aCtInterface.setPosition(clonePosition(intrface.getPosition()));
		aCtInterface.setExtendedModifiers(cloneModifiers(intrface,aCtInterface));
		aCtInterface.setAnnotations(this.cloneHelper.clone(intrface.getAnnotations()));
		aCtInterface.setSuperInterfaces(this.cloneHelper.clone(intrface.getSuperInterfaces()));
		aCtInterface.setFormalCtTypeParameters(this.cloneHelper.clone(intrface.getFormalCtTypeParameters()));
		aCtInterface.setTypeMembers(this.cloneHelper.clone(intrface.getTypeMembers()));
		aCtInterface.setComments(this.cloneHelper.clone(intrface.getComments()));
		if (aCtInterface.isTopLevel()) {
			aCtInterface.getPosition().getCompilationUnit().addDeclaredType(aCtInterface);
		}
		this.cloneHelper.tailor(intrface, aCtInterface);
		this.other = aCtInterface;
	}

	public <T> void visitCtInvocation(final CtInvocation<T> invocation) {
		CtInvocation<T> aCtInvocation = this.factory.Core().createInvocation();
		this.builder.copy(invocation, aCtInvocation);
		aCtInvocation.setPosition(clonePosition(invocation.getPosition()));
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
		this.builder.copy(literal, aCtLiteral);
		aCtLiteral.setPosition(clonePosition(literal.getPosition()));
		aCtLiteral.setAnnotations(this.cloneHelper.clone(literal.getAnnotations()));
		aCtLiteral.setType(this.cloneHelper.clone(literal.getType()));
		aCtLiteral.setTypeCasts(this.cloneHelper.clone(literal.getTypeCasts()));
		aCtLiteral.setComments(this.cloneHelper.clone(literal.getComments()));
		this.cloneHelper.tailor(literal, aCtLiteral);
		this.other = aCtLiteral;
	}

	public <T> void visitCtLocalVariable(final CtLocalVariable<T> localVariable) {
		CtLocalVariable<T> aCtLocalVariable = this.factory.Core().createLocalVariable();
		this.builder.copy(localVariable, aCtLocalVariable);
		aCtLocalVariable.setPosition(clonePosition(localVariable.getPosition()));
		aCtLocalVariable.setExtendedModifiers(cloneModifiers(localVariable,aCtLocalVariable));
		aCtLocalVariable.setAnnotations(this.cloneHelper.clone(localVariable.getAnnotations()));
		aCtLocalVariable.setType(this.cloneHelper.clone(localVariable.getType()));
		aCtLocalVariable.setDefaultExpression(this.cloneHelper.clone(localVariable.getDefaultExpression()));
		aCtLocalVariable.setComments(this.cloneHelper.clone(localVariable.getComments()));
		this.cloneHelper.tailor(localVariable, aCtLocalVariable);
		this.other = aCtLocalVariable;
	}

	public <T> void visitCtLocalVariableReference(final CtLocalVariableReference<T> reference) {
		CtLocalVariableReference<T> aCtLocalVariableReference = this.factory.Core().createLocalVariableReference();
		this.builder.copy(reference, aCtLocalVariableReference);
		aCtLocalVariableReference.setPosition(clonePosition(reference.getPosition()));
		aCtLocalVariableReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtLocalVariableReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtLocalVariableReference);
		this.other = aCtLocalVariableReference;
	}

	public <T> void visitCtCatchVariable(final CtCatchVariable<T> catchVariable) {
		CtCatchVariable<T> aCtCatchVariable = this.factory.Core().createCatchVariable();
		this.builder.copy(catchVariable, aCtCatchVariable);
		aCtCatchVariable.setPosition(clonePosition(catchVariable.getPosition()));
		aCtCatchVariable.setExtendedModifiers(cloneModifiers(catchVariable,aCtCatchVariable));
		aCtCatchVariable.setComments(this.cloneHelper.clone(catchVariable.getComments()));
		aCtCatchVariable.setAnnotations(this.cloneHelper.clone(catchVariable.getAnnotations()));
		aCtCatchVariable.setMultiTypes(this.cloneHelper.clone(catchVariable.getMultiTypes()));
		this.cloneHelper.tailor(catchVariable, aCtCatchVariable);
		this.other = aCtCatchVariable;
	}

	public <T> void visitCtCatchVariableReference(final CtCatchVariableReference<T> reference) {
		CtCatchVariableReference<T> aCtCatchVariableReference = this.factory.Core().createCatchVariableReference();
		this.builder.copy(reference, aCtCatchVariableReference);
		aCtCatchVariableReference.setPosition(clonePosition(reference.getPosition()));
		aCtCatchVariableReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtCatchVariableReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtCatchVariableReference);
		this.other = aCtCatchVariableReference;
	}

	public <T> void visitCtMethod(final CtMethod<T> m) {
		CtMethod<T> aCtMethod = this.factory.Core().createMethod();
		this.builder.copy(m, aCtMethod);
		aCtMethod.setPosition(clonePosition(m.getPosition()));
		aCtMethod.setExtendedModifiers(cloneModifiers(m,aCtMethod));
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

	private <T extends CtModifiable> Set<CtExtendedModifier> cloneModifiers(final T m, T aCtMethod) {
		return this.cloneHelper.clone(m.getExtendedModifiers().stream()
				.map(x -> {
					// TODO do not access GUMTREE_NODE
					ITree aaa = (ITree)m.getMetadata(SpoonGumTreeBuilder.GUMTREE_NODE);
					if (aaa!=null) {
						for (ITree y : aaa.getChildren()) {
							Object yy = (Object)y.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
							if (yy instanceof CtWrapper && ((CtWrapper)yy).getValue()==x) {
								CtWrapper<CtExtendedModifier> r = (CtWrapper) yy;
								r.putMetadata("clone", aCtMethod);
								return r;
							}
						}
					}
					CtWrapper<CtExtendedModifier> r = new CtWrapper<>(x, m, CtRole.MODIFIER);
					r.putMetadata("clone", aCtMethod);
					return r;
				}).collect(Collectors.toList())).stream()
				.map(x -> x.getValue()).collect(Collectors.toSet());
	}
	
	// aCtMethod.setExtendedModifiers(cloneModaifiers(m, aCtMethod));
	//aCtMethod.setExtendedModifiers(cloneModaifiers(m.getExtendedModifiers()));
	public <T> void visitCtWrapper(CtWrapper<T> orig) {
		
		if (orig.getValue() instanceof CtExtendedModifier) {
			CtExtendedModifier v = ((CtWrapper<CtExtendedModifier>)orig).getValue();
			CtExtendedModifier wrapped = new CtExtendedModifier(v.getKind(), v.isImplicit());
			this.other = new CtWrapper<CtExtendedModifier>(wrapped,(CtElement)orig.getMetadata("clone"),CtRole.MODIFIER);
			this.other.setAllMetadata(orig.getAllMetadata());
			this.other.setPosition(clonePosition(orig.getPosition()));
		}
	}

	@java.lang.Override
	public <T> void visitCtAnnotationMethod(CtAnnotationMethod<T> annotationMethod) {
		CtAnnotationMethod<T> aCtAnnotationMethod = this.factory.Core().createAnnotationMethod();
		this.builder.copy(annotationMethod, aCtAnnotationMethod);
		aCtAnnotationMethod.setPosition(clonePosition(annotationMethod.getPosition()));
		aCtAnnotationMethod.setAnnotations(this.cloneHelper.clone(annotationMethod.getAnnotations()));
		aCtAnnotationMethod.setType(this.cloneHelper.clone(annotationMethod.getType()));
		aCtAnnotationMethod.setDefaultExpression(this.cloneHelper.clone(annotationMethod.getDefaultExpression()));
		aCtAnnotationMethod.setComments(this.cloneHelper.clone(annotationMethod.getComments()));
		this.cloneHelper.tailor(annotationMethod, aCtAnnotationMethod);
		this.other = aCtAnnotationMethod;
	}

	public <T> void visitCtNewArray(final CtNewArray<T> newArray) {
		CtNewArray<T> aCtNewArray = this.factory.Core().createNewArray();
		this.builder.copy(newArray, aCtNewArray);
		aCtNewArray.setPosition(clonePosition(newArray.getPosition()));
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
		this.builder.copy(ctConstructorCall, aCtConstructorCall);
		aCtConstructorCall.setPosition(clonePosition(ctConstructorCall.getPosition()));
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
		this.builder.copy(newClass, aCtNewClass);
		aCtNewClass.setPosition(clonePosition(newClass.getPosition()));
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
		this.builder.copy(lambda, aCtLambda);
		aCtLambda.setPosition(clonePosition(lambda.getPosition()));
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
		CtExecutableReferenceExpression<T, E> aCtExecutableReferenceExpression = this.factory.Core()
				.createExecutableReferenceExpression();
		this.builder.copy(expression, aCtExecutableReferenceExpression);
		aCtExecutableReferenceExpression.setPosition(clonePosition(expression.getPosition()));
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
		this.builder.copy(assignment, aCtOperatorAssignment);
		aCtOperatorAssignment.setPosition(clonePosition(assignment.getPosition()));
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
		CtPackage aCtPackage = ctPackage instanceof CtRootPackage ? this.factory.getModel().getRootPackage()
				: this.factory.Core().createPackage();
		this.builder.copy(ctPackage, aCtPackage);
		aCtPackage.setPosition(clonePosition(ctPackage.getPosition()));
		aCtPackage.setAnnotations(this.cloneHelper.clone(ctPackage.getAnnotations()));
		aCtPackage.setPackages(this.cloneHelper.clone(ctPackage.getPackages()));
		aCtPackage.setTypes(this.cloneHelper.clone(ctPackage.getTypes()));
		aCtPackage.setComments(this.cloneHelper.clone(ctPackage.getComments()));
		aCtPackage.getPosition().getCompilationUnit().setDeclaredPackage(aCtPackage);
		this.cloneHelper.tailor(ctPackage, aCtPackage);
		this.other = aCtPackage;
	}

	public void visitCtPackageReference(final CtPackageReference reference) {
		CtPackageReference aCtPackageReference = this.factory.Core().createPackageReference();
		this.builder.copy(reference, aCtPackageReference);
		aCtPackageReference.setPosition(clonePosition(reference.getPosition()));
		aCtPackageReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtPackageReference);
		this.other = aCtPackageReference;
	}

	public <T> void visitCtParameter(final CtParameter<T> parameter) {
		CtParameter<T> aCtParameter = this.factory.Core().createParameter();
		this.builder.copy(parameter, aCtParameter);
		aCtParameter.setPosition(clonePosition(parameter.getPosition()));
		aCtParameter.setExtendedModifiers(cloneModifiers(parameter,aCtParameter));
		aCtParameter.setAnnotations(this.cloneHelper.clone(parameter.getAnnotations()));
		aCtParameter.setType(this.cloneHelper.clone(parameter.getType()));
		aCtParameter.setComments(this.cloneHelper.clone(parameter.getComments()));
		this.cloneHelper.tailor(parameter, aCtParameter);
		this.other = aCtParameter;
	}

	public <T> void visitCtParameterReference(final CtParameterReference<T> reference) {
		CtParameterReference<T> aCtParameterReference = this.factory.Core().createParameterReference();
		this.builder.copy(reference, aCtParameterReference);
		aCtParameterReference.setPosition(clonePosition(reference.getPosition()));
		aCtParameterReference.setType(this.cloneHelper.clone(reference.getType()));
		aCtParameterReference.setAnnotations(this.cloneHelper.clone(reference.getAnnotations()));
		this.cloneHelper.tailor(reference, aCtParameterReference);
		this.other = aCtParameterReference;
	}

	public <R> void visitCtReturn(final CtReturn<R> returnStatement) {
		CtReturn<R> aCtReturn = this.factory.Core().createReturn();
		this.builder.copy(returnStatement, aCtReturn);
		aCtReturn.setPosition(clonePosition(returnStatement.getPosition()));
		aCtReturn.setAnnotations(this.cloneHelper.clone(returnStatement.getAnnotations()));
		aCtReturn.setReturnedExpression(this.cloneHelper.clone(returnStatement.getReturnedExpression()));
		aCtReturn.setComments(this.cloneHelper.clone(returnStatement.getComments()));
		this.cloneHelper.tailor(returnStatement, aCtReturn);
		this.other = aCtReturn;
	}

	public <R> void visitCtStatementList(final CtStatementList statements) {
		CtStatementList aCtStatementList = this.factory.Core().createStatementList();
		this.builder.copy(statements, aCtStatementList);
		aCtStatementList.setPosition(clonePosition(statements.getPosition()));
		aCtStatementList.setAnnotations(this.cloneHelper.clone(statements.getAnnotations()));
		aCtStatementList.setStatements(this.cloneHelper.clone(statements.getStatements()));
		aCtStatementList.setComments(this.cloneHelper.clone(statements.getComments()));
		this.cloneHelper.tailor(statements, aCtStatementList);
		this.other = aCtStatementList;
	}

	public <S> void visitCtSwitch(final CtSwitch<S> switchStatement) {
		CtSwitch<S> aCtSwitch = this.factory.Core().createSwitch();
		this.builder.copy(switchStatement, aCtSwitch);
		aCtSwitch.setPosition(clonePosition(switchStatement.getPosition()));
		aCtSwitch.setAnnotations(this.cloneHelper.clone(switchStatement.getAnnotations()));
		aCtSwitch.setSelector(this.cloneHelper.clone(switchStatement.getSelector()));
		aCtSwitch.setCases(this.cloneHelper.clone(switchStatement.getCases()));
		aCtSwitch.setComments(this.cloneHelper.clone(switchStatement.getComments()));
		this.cloneHelper.tailor(switchStatement, aCtSwitch);
		this.other = aCtSwitch;
	}

	public void visitCtSynchronized(final CtSynchronized synchro) {
		CtSynchronized aCtSynchronized = this.factory.Core().createSynchronized();
		this.builder.copy(synchro, aCtSynchronized);
		aCtSynchronized.setPosition(clonePosition(synchro.getPosition()));
		aCtSynchronized.setAnnotations(this.cloneHelper.clone(synchro.getAnnotations()));
		aCtSynchronized.setExpression(this.cloneHelper.clone(synchro.getExpression()));
		aCtSynchronized.setBlock(this.cloneHelper.clone(synchro.getBlock()));
		aCtSynchronized.setComments(this.cloneHelper.clone(synchro.getComments()));
		this.cloneHelper.tailor(synchro, aCtSynchronized);
		this.other = aCtSynchronized;
	}

	public void visitCtThrow(final CtThrow throwStatement) {
		CtThrow aCtThrow = this.factory.Core().createThrow();
		this.builder.copy(throwStatement, aCtThrow);
		aCtThrow.setPosition(clonePosition(throwStatement.getPosition()));
		aCtThrow.setAnnotations(this.cloneHelper.clone(throwStatement.getAnnotations()));
		aCtThrow.setThrownExpression(this.cloneHelper.clone(throwStatement.getThrownExpression()));
		aCtThrow.setComments(this.cloneHelper.clone(throwStatement.getComments()));
		this.cloneHelper.tailor(throwStatement, aCtThrow);
		this.other = aCtThrow;
	}

	public void visitCtTry(final CtTry tryBlock) {
		CtTry aCtTry = this.factory.Core().createTry();
		this.builder.copy(tryBlock, aCtTry);
		aCtTry.setPosition(clonePosition(tryBlock.getPosition()));
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
		this.builder.copy(tryWithResource, aCtTryWithResource);
		aCtTryWithResource.setPosition(clonePosition(tryWithResource.getPosition()));
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
		this.builder.copy(ref, aCtTypeParameterReference);
		aCtTypeParameterReference.setPosition(clonePosition(ref.getPosition()));
		aCtTypeParameterReference.setPackage(this.cloneHelper.clone(ref.getPackage()));
		aCtTypeParameterReference.setDeclaringType(this.cloneHelper.clone(ref.getDeclaringType()));
		aCtTypeParameterReference.setAnnotations(this.cloneHelper.clone(ref.getAnnotations()));
		this.cloneHelper.tailor(ref, aCtTypeParameterReference);
		this.other = aCtTypeParameterReference;
	}

	@java.lang.Override
	public void visitCtWildcardReference(CtWildcardReference wildcardReference) {
		CtWildcardReference aCtWildcardReference = this.factory.Core().createWildcardReference();
		this.builder.copy(wildcardReference, aCtWildcardReference);
		aCtWildcardReference.setPosition(clonePosition(wildcardReference.getPosition()));
		aCtWildcardReference.setPackage(this.cloneHelper.clone(wildcardReference.getPackage()));
		aCtWildcardReference.setDeclaringType(this.cloneHelper.clone(wildcardReference.getDeclaringType()));
		aCtWildcardReference.setAnnotations(this.cloneHelper.clone(wildcardReference.getAnnotations()));
		aCtWildcardReference.setBoundingType(this.cloneHelper.clone(wildcardReference.getBoundingType()));
		this.cloneHelper.tailor(wildcardReference, aCtWildcardReference);
		this.other = aCtWildcardReference;
	}

	@java.lang.Override
	public <T> void visitCtIntersectionTypeReference(final CtIntersectionTypeReference<T> reference) {
		CtIntersectionTypeReference<T> aCtIntersectionTypeReference = this.factory.Core()
				.createIntersectionTypeReference();
		this.builder.copy(reference, aCtIntersectionTypeReference);
		aCtIntersectionTypeReference.setPosition(clonePosition(reference.getPosition()));
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
		this.builder.copy(reference, aCtTypeReference);
		aCtTypeReference.setPosition(clonePosition(reference.getPosition()));
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
		this.builder.copy(typeAccess, aCtTypeAccess);
		aCtTypeAccess.setPosition(clonePosition(typeAccess.getPosition()));
		aCtTypeAccess.setAnnotations(this.cloneHelper.clone(typeAccess.getAnnotations()));
		aCtTypeAccess.setTypeCasts(this.cloneHelper.clone(typeAccess.getTypeCasts()));
		aCtTypeAccess.setAccessedType(this.cloneHelper.clone(typeAccess.getAccessedType()));
		aCtTypeAccess.setComments(this.cloneHelper.clone(typeAccess.getComments()));
		this.cloneHelper.tailor(typeAccess, aCtTypeAccess);
		this.other = aCtTypeAccess;
	}

	public <T> void visitCtUnaryOperator(final CtUnaryOperator<T> operator) {
		CtUnaryOperator<T> aCtUnaryOperator = this.factory.Core().createUnaryOperator();
		this.builder.copy(operator, aCtUnaryOperator);
		aCtUnaryOperator.setPosition(clonePosition(operator.getPosition()));
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
		this.builder.copy(variableRead, aCtVariableRead);
		aCtVariableRead.setPosition(clonePosition(variableRead.getPosition()));
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
		this.builder.copy(variableWrite, aCtVariableWrite);
		aCtVariableWrite.setPosition(clonePosition(variableWrite.getPosition()));
		aCtVariableWrite.setAnnotations(this.cloneHelper.clone(variableWrite.getAnnotations()));
		aCtVariableWrite.setTypeCasts(this.cloneHelper.clone(variableWrite.getTypeCasts()));
		aCtVariableWrite.setVariable(this.cloneHelper.clone(variableWrite.getVariable()));
		aCtVariableWrite.setComments(this.cloneHelper.clone(variableWrite.getComments()));
		this.cloneHelper.tailor(variableWrite, aCtVariableWrite);
		this.other = aCtVariableWrite;
	}

	public void visitCtWhile(final CtWhile whileLoop) {
		CtWhile aCtWhile = this.factory.Core().createWhile();
		this.builder.copy(whileLoop, aCtWhile);
		aCtWhile.setPosition(clonePosition(whileLoop.getPosition()));
		aCtWhile.setAnnotations(this.cloneHelper.clone(whileLoop.getAnnotations()));
		aCtWhile.setLoopingExpression(this.cloneHelper.clone(whileLoop.getLoopingExpression()));
		aCtWhile.setBody(this.cloneHelper.clone(whileLoop.getBody()));
		if (whileLoop.getBody() != null) {
			aCtWhile.getBody().setImplicit(whileLoop.getBody().isImplicit());
		}
		aCtWhile.setComments(this.cloneHelper.clone(whileLoop.getComments()));
		this.cloneHelper.tailor(whileLoop, aCtWhile);
		this.other = aCtWhile;
	}

	public <T> void visitCtCodeSnippetExpression(final CtCodeSnippetExpression<T> expression) {
		CtCodeSnippetExpression<T> aCtCodeSnippetExpression = this.factory.Core().createCodeSnippetExpression();
		this.builder.copy(expression, aCtCodeSnippetExpression);
		aCtCodeSnippetExpression.setPosition(clonePosition(expression.getPosition()));
		aCtCodeSnippetExpression.setType(this.cloneHelper.clone(expression.getType()));
		aCtCodeSnippetExpression.setComments(this.cloneHelper.clone(expression.getComments()));
		aCtCodeSnippetExpression.setAnnotations(this.cloneHelper.clone(expression.getAnnotations()));
		aCtCodeSnippetExpression.setTypeCasts(this.cloneHelper.clone(expression.getTypeCasts()));
		this.cloneHelper.tailor(expression, aCtCodeSnippetExpression);
		this.other = aCtCodeSnippetExpression;
	}

	public void visitCtCodeSnippetStatement(final CtCodeSnippetStatement statement) {
		CtCodeSnippetStatement aCtCodeSnippetStatement = this.factory.Core().createCodeSnippetStatement();
		this.builder.copy(statement, aCtCodeSnippetStatement);
		aCtCodeSnippetStatement.setPosition(clonePosition(statement.getPosition()));
		aCtCodeSnippetStatement.setComments(this.cloneHelper.clone(statement.getComments()));
		aCtCodeSnippetStatement.setAnnotations(this.cloneHelper.clone(statement.getAnnotations()));
		this.cloneHelper.tailor(statement, aCtCodeSnippetStatement);
		this.other = aCtCodeSnippetStatement;
	}

	public <T> void visitCtUnboundVariableReference(final CtUnboundVariableReference<T> reference) {
		CtUnboundVariableReference<T> aCtUnboundVariableReference = this.factory.Core()
				.createUnboundVariableReference();
		this.builder.copy(reference, aCtUnboundVariableReference);
		aCtUnboundVariableReference.setPosition(clonePosition(reference.getPosition()));
		aCtUnboundVariableReference.setType(this.cloneHelper.clone(reference.getType()));
		this.cloneHelper.tailor(reference, aCtUnboundVariableReference);
		this.other = aCtUnboundVariableReference;
	}

	@java.lang.Override
	public <T> void visitCtFieldRead(final CtFieldRead<T> fieldRead) {
		CtFieldRead<T> aCtFieldRead = this.factory.Core().createFieldRead();
		this.builder.copy(fieldRead, aCtFieldRead);
		aCtFieldRead.setPosition(clonePosition(fieldRead.getPosition()));
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
		this.builder.copy(fieldWrite, aCtFieldWrite);
		aCtFieldWrite.setPosition(clonePosition(fieldWrite.getPosition()));
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
		this.builder.copy(f, aCtSuperAccess);
		aCtSuperAccess.setPosition(clonePosition(f.getPosition()));
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
		this.builder.copy(comment, aCtComment);
		aCtComment.setPosition(clonePosition(comment.getPosition()));
		aCtComment.setComments(this.cloneHelper.clone(comment.getComments()));
		aCtComment.setAnnotations(this.cloneHelper.clone(comment.getAnnotations()));
		this.cloneHelper.tailor(comment, aCtComment);
		this.other = aCtComment;
	}

	@java.lang.Override
	public void visitCtJavaDoc(final CtJavaDoc javaDoc) {
		CtJavaDoc aCtJavaDoc = this.factory.Core().createJavaDoc();
		this.builder.copy(javaDoc, aCtJavaDoc);
		aCtJavaDoc.setPosition(clonePosition(javaDoc.getPosition()));
		aCtJavaDoc.setComments(this.cloneHelper.clone(javaDoc.getComments()));
		aCtJavaDoc.setAnnotations(this.cloneHelper.clone(javaDoc.getAnnotations()));
		aCtJavaDoc.setTags(this.cloneHelper.clone(javaDoc.getTags()));
		this.cloneHelper.tailor(javaDoc, aCtJavaDoc);
		this.other = aCtJavaDoc;
	}

	@java.lang.Override
	public void visitCtJavaDocTag(final CtJavaDocTag docTag) {
		CtJavaDocTag aCtJavaDocTag = this.factory.Core().createJavaDocTag();
		this.builder.copy(docTag, aCtJavaDocTag);
		aCtJavaDocTag.setPosition(clonePosition(docTag.getPosition()));
		aCtJavaDocTag.setComments(this.cloneHelper.clone(docTag.getComments()));
		aCtJavaDocTag.setAnnotations(this.cloneHelper.clone(docTag.getAnnotations()));
		this.cloneHelper.tailor(docTag, aCtJavaDocTag);
		this.other = aCtJavaDocTag;
	}

	@java.lang.Override
	public void visitCtImport(final CtImport ctImport) {
		if (ctImport instanceof CtUnresolvedImport) {
			CtImport aCtImport = this.factory.Core().createUnresolvedImport();
			this.builder.copy(ctImport, aCtImport);
			aCtImport.setPosition(clonePosition(ctImport.getPosition()));
			((CtUnresolvedImport)aCtImport).setUnresolvedReference(((CtUnresolvedImport)ctImport).getUnresolvedReference());
			aCtImport.setAnnotations(this.cloneHelper.clone(ctImport.getAnnotations()));
			aCtImport.setComments(this.cloneHelper.clone(ctImport.getComments()));
			this.cloneHelper.tailor(ctImport, aCtImport);
			this.other = aCtImport;
		} else {
			CtImport aCtImport = this.factory.Core().createImport();
			this.builder.copy(ctImport, aCtImport);
			aCtImport.setPosition(clonePosition(ctImport.getPosition()));
			aCtImport.setReference(assertNotNull(this.cloneHelper.clone(ctImport.getReference())));
			aCtImport.setAnnotations(this.cloneHelper.clone(ctImport.getAnnotations()));
			aCtImport.setComments(this.cloneHelper.clone(ctImport.getComments()));
			this.cloneHelper.tailor(ctImport, aCtImport);
			this.other = aCtImport;
		}
	}
	

	private CtReference assertNotNull(CtReference clone) {
		if (clone == null) {
			throw new RuntimeException("reference is null");
		}
		return clone;
	}

	@java.lang.Override
	public void visitCtModule(CtModule module) {
		CtModule aCtModule = this.factory.Core().createModule();
		this.builder.copy(module, aCtModule);
		aCtModule.setPosition(clonePosition(module.getPosition()));
		aCtModule.setComments(this.cloneHelper.clone(module.getComments()));
		aCtModule.setAnnotations(this.cloneHelper.clone(module.getAnnotations()));
		aCtModule.setModuleDirectives(this.cloneHelper.clone(module.getModuleDirectives()));
		aCtModule.setRootPackage(this.cloneHelper.clone(module.getRootPackage()));
		aCtModule.getPosition().getCompilationUnit().setDeclaredModule(aCtModule);
		this.cloneHelper.tailor(module, aCtModule);
		this.other = aCtModule;
	}

	@java.lang.Override
	public void visitCtModuleReference(CtModuleReference moduleReference) {
		CtModuleReference aCtModuleReference = this.factory.Core().createModuleReference();
		this.builder.copy(moduleReference, aCtModuleReference);
		aCtModuleReference.setPosition(clonePosition(moduleReference.getPosition()));
		aCtModuleReference.setAnnotations(this.cloneHelper.clone(moduleReference.getAnnotations()));
		this.cloneHelper.tailor(moduleReference, aCtModuleReference);
		this.other = aCtModuleReference;
	}

	@java.lang.Override
	public void visitCtPackageExport(CtPackageExport moduleExport) {
		CtPackageExport aCtPackageExport = this.factory.Core().createPackageExport();
		this.builder.copy(moduleExport, aCtPackageExport);
		aCtPackageExport.setPosition(clonePosition(moduleExport.getPosition()));
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
		this.builder.copy(moduleRequirement, aCtModuleRequirement);
		aCtModuleRequirement.setPosition(clonePosition(moduleRequirement.getPosition()));
		aCtModuleRequirement.setComments(this.cloneHelper.clone(moduleRequirement.getComments()));
		aCtModuleRequirement.setModuleReference(this.cloneHelper.clone(moduleRequirement.getModuleReference()));
		aCtModuleRequirement.setAnnotations(this.cloneHelper.clone(moduleRequirement.getAnnotations()));
		this.cloneHelper.tailor(moduleRequirement, aCtModuleRequirement);
		this.other = aCtModuleRequirement;
	}

	@java.lang.Override
	public void visitCtProvidedService(CtProvidedService moduleProvidedService) {
		CtProvidedService aCtProvidedService = this.factory.Core().createProvidedService();
		this.builder.copy(moduleProvidedService, aCtProvidedService);
		aCtProvidedService.setPosition(clonePosition(moduleProvidedService.getPosition()));
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
		this.builder.copy(usedService, aCtUsedService);
		aCtUsedService.setPosition(clonePosition(usedService.getPosition()));
		aCtUsedService.setComments(this.cloneHelper.clone(usedService.getComments()));
		aCtUsedService.setServiceType(this.cloneHelper.clone(usedService.getServiceType()));
		aCtUsedService.setAnnotations(this.cloneHelper.clone(usedService.getAnnotations()));
		this.cloneHelper.tailor(usedService, aCtUsedService);
		this.other = aCtUsedService;
	}

	@java.lang.Override
	public void visitCtCompilationUnit(CtCompilationUnit compilationUnit) {
		String path = compilationUnit.getFile()==null ? null:compilationUnit.getFile().getPath();
		CtCompilationUnit aCtCompilationUnit = this.factory.CompilationUnit().getMap().get(path);
		if (aCtCompilationUnit == null) {
			aCtCompilationUnit = this.factory.Core().createCompilationUnit();
			this.factory.CompilationUnit().getMap().put(path, (CompilationUnit)aCtCompilationUnit);
		} else {
			this.other = aCtCompilationUnit;
			return;
		}
		this.builder.copy(compilationUnit, aCtCompilationUnit);
		// CAUTION do not clone position here even if there is one, cause an infinite loop
		aCtCompilationUnit.setComments(this.cloneHelper.clone(compilationUnit.getComments()));
		aCtCompilationUnit.setAnnotations(this.cloneHelper.clone(compilationUnit.getAnnotations()));
		aCtCompilationUnit.setPackageDeclaration(this.cloneHelper.clone(compilationUnit.getPackageDeclaration()));
		aCtCompilationUnit.setImports(this.cloneHelper.clone(compilationUnit.getImports()));
		aCtCompilationUnit
				.setDeclaredModuleReference(this.cloneHelper.clone(compilationUnit.getDeclaredModuleReference()));
		aCtCompilationUnit
				.setDeclaredTypeReferences(this.cloneHelper.clone(compilationUnit.getDeclaredTypeReferences()));
		aCtCompilationUnit.putMetadata("SourceTypeNRootDirectory",
				compilationUnit.getMetadata("SourceTypeNRootDirectory"));
		this.cloneHelper.tailor(compilationUnit, aCtCompilationUnit);
		this.other = aCtCompilationUnit;
	}

	@java.lang.Override
	public void visitCtPackageDeclaration(CtPackageDeclaration packageDeclaration) {
		CtPackageDeclaration aCtPackageDeclaration = this.factory.Core().createPackageDeclaration();
		this.builder.copy(packageDeclaration, aCtPackageDeclaration);
		aCtPackageDeclaration.setPosition(clonePosition(packageDeclaration.getPosition()));
		aCtPackageDeclaration.setComments(this.cloneHelper.clone(packageDeclaration.getComments()));
		aCtPackageDeclaration.setAnnotations(this.cloneHelper.clone(packageDeclaration.getAnnotations()));
		aCtPackageDeclaration.setReference(this.cloneHelper.clone(packageDeclaration.getReference()));
		this.cloneHelper.tailor(packageDeclaration, aCtPackageDeclaration);
		this.other = aCtPackageDeclaration;
	}

	@java.lang.Override
	public void visitCtTypeMemberWildcardImportReference(CtTypeMemberWildcardImportReference wildcardReference) {
		CtTypeMemberWildcardImportReference aCtTypeMemberWildcardImportReference = this.factory.Core()
				.createTypeMemberWildcardImportReference();
		this.builder.copy(wildcardReference, aCtTypeMemberWildcardImportReference);
		aCtTypeMemberWildcardImportReference.setPosition(clonePosition(wildcardReference.getPosition()));
		aCtTypeMemberWildcardImportReference
				.setTypeReference(this.cloneHelper.clone(wildcardReference.getTypeReference()));
		this.cloneHelper.tailor(wildcardReference, aCtTypeMemberWildcardImportReference);
		this.other = aCtTypeMemberWildcardImportReference;
	}
}