package gumtree.spoon.apply;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.gumtreediff.actions.MyAction.MyDelete;
import com.github.gumtreediff.actions.MyAction.MyInsert;
import com.github.gumtreediff.actions.MyAction.MyUpdate;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.VersionedTree;

import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.compiler.Environment;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.*;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.filter.PotentialVariableDeclarationFunction;
import spoon.support.StandardEnvironment;
import spoon.support.reflect.CtExtendedModifier;

public class ActionApplier {
	static Logger LOGGER = Logger.getLogger("ApplyTestHelper");
	static Environment env = new StandardEnvironment();
	static spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);

	public static void applyMyInsert(Factory factory, TreeContext ctx, MyInsert action)
			throws WrongAstContextException, MissingParentException {
		ITree source = action.getSource();
		factory.createLocalVariableReference().getDeclaration();
		AbstractVersionedTree target = action.getTarget();
		AbstractVersionedTree parentTarget = target.getParent();
		String targetType = (String) target.getMetadata("type");
		target.setMetadata("inserted", false);
		switch (targetType) {
			case "LABEL": {
				CtElement parent = getSpoonEleStrict(parentTarget);
				if (parent == null) {
					LOGGER.warning("no parent for label " + target.getLabel());
				} else if (parent instanceof CtExecutableReferenceExpression) {
					CtExecutableReference ref = factory.createExecutableReference();
					ref.setSimpleName(target.getLabel());
					((CtExecutableReferenceExpression) parent).setExecutable(ref);
				} else if (parent instanceof CtNamedElement) {
					((CtNamedElement) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtWildcardReference) {
				} else if (parent instanceof CtTypeReference) {
					CtTypeReference sps = getSpoonEle(source.getParent());
					CtTypeReference ref = factory.Type().createReference(sps.getQualifiedName());
					CtPackageReference pack = ref.getPackage();
					if (((CtTypeReference<?>) parent).getPackage() == null && pack != null) {
						((CtTypeReference<?>) parent).setPackage(pack);
						pack.setImplicit(true);
						pack.setParent(parent);
					}
					((CtTypeReference<?>) parent).setSimpleName(target.getLabel());
					// if (parent.isParentInitialized()) {
					// 	CtElement parentparent = parent.getParent();
					// 	if (parentparent instanceof CtAnnotation) {
					// 		((CtAnnotation) parentparent).setType((CtTypeReference) parent);
					// 	}
					// }
				} else if (parent instanceof CtBinaryOperator) {
					((CtBinaryOperator<?>) parent).setKind(BinaryOperatorKind.valueOf(target.getLabel()));
				} else if (parent instanceof CtUnaryOperator) {
					((CtUnaryOperator<?>) parent).setKind(UnaryOperatorKind.valueOf(target.getLabel()));
				} else if (parent instanceof CtLiteral) {
					if (target.getLabel().startsWith("\"")) {
						((CtLiteral<String>) parent).setValue(target.getLabel()
								.substring(1, target.getLabel().length() - 1).replace("\\\\", "\\").toString());
						((CtLiteral<String>) parent).setType(factory.Type().STRING);
						CtLiteral sl = getSpoonEle(source.getParent());
						int len = sl.getPosition() != null && sl.getPosition().isValidPosition()
								? sl.getPosition().getSourceEnd() - sl.getPosition().getSourceStart()
								: ((CtLiteral<String>) parent).getValue().length();
						((CtLiteral<String>) parent).setPosition(new NoSourcePosition() {
							@Override
							public boolean isValidPosition() {
								return true;
							}

							@Override
							public int getSourceStart() {
								return 0;
							}

							@Override
							public int getSourceEnd() {
								return len;
							}
						});
					} else if (target.getLabel().startsWith("'")) {
						((CtLiteral<Character>) parent)
								.setValue(target.getLabel().substring(1, target.getLabel().length() - 1).charAt(0));
						((CtLiteral<Character>) parent).setType(factory.Type().CHARACTER_PRIMITIVE);
						CtLiteral sl = getSpoonEle(source.getParent());
						int len = sl.getPosition() != null && sl.getPosition().isValidPosition()
								? sl.getPosition().getSourceEnd() - sl.getPosition().getSourceStart()
								: ((CtLiteral<Character>) parent).getValue().toString().length();
						((CtLiteral<String>) parent).setPosition(new NoSourcePosition() {
							@Override
							public boolean isValidPosition() {
								return true;
							}

							@Override
							public int getSourceStart() {
								return 0;
							}

							@Override
							public int getSourceEnd() {
								return len;
							}
						});
					} else if (target.getLabel().equals("true")) {
						((CtLiteral<Boolean>) parent).setValue(true);
					} else if (target.getLabel().equals("false")) {
						((CtLiteral<Boolean>) parent).setValue(false);
					} else if (target.getLabel().equals("null")) {
						((CtLiteral<Object>) parent).setValue(null);
					} else if (target.getLabel().endsWith("F")) {
						((CtLiteral<Float>) parent).setValue(
								Float.parseFloat(target.getLabel().substring(0, target.getLabel().length() - 1)));
					} else if (target.getLabel().endsWith("L")) {
						if (target.getLabel().startsWith("0x")) {
							((CtLiteral) parent).setBase(LiteralBase.HEXADECIMAL);
							((CtLiteral<Long>) parent).setValue(
									Long.decode(target.getLabel().substring(0, target.getLabel().length() - 1)));
						} else {
							((CtLiteral<Long>) parent).setValue(
									Long.parseLong(target.getLabel().substring(0, target.getLabel().length() - 1)));
						}
					} else if (target.getLabel().endsWith("D")) {
						((CtLiteral<Double>) parent).setValue(
								Double.parseDouble(target.getLabel().substring(0, target.getLabel().length())));
					} else {
						if (target.getLabel().startsWith("0x")) {
							((CtLiteral) parent).setBase(LiteralBase.HEXADECIMAL);
						}
						try {
							((CtLiteral<Integer>) parent).setValue(Integer.decode(target.getLabel()));
						} catch (Exception e) {
							try {
								((CtLiteral<Long>) parent).setValue(Long.decode(target.getLabel()));
							} catch (Exception ee) {
								((CtLiteral<Double>) parent).setValue(Double.parseDouble(target.getLabel()));
							}
						}
					}
				} else if (parent instanceof CtFieldAccess) {
					CtField var;
					try {
						var = factory.Query().createQuery(parent)
								.map(new PotentialVariableDeclarationFunction(target.getLabel())).first(CtField.class);
					} catch (Exception e) {
						var = null;
					}

					// CtVariableReference ref = factory.createFieldReference();
					// ref.setSimpleName(target.getLabel());
					if (var == null) {
						CtFieldAccess sps = getSpoonEle(source.getParent());
						CtFieldReference field = sps.getVariable().getType() == null
								? factory.Field()
										.createReference(factory.Type().NULL_TYPE.toString() + " "
												+ sps.getVariable().getQualifiedName())
								: factory.Field().createReference(sps.getVariable().getType().getQualifiedName() + " "
										+ sps.getVariable().getQualifiedName());
						((CtFieldAccess<?>) parent).setVariable(field);
						if (!field.getDeclaringType().getSimpleName().equals("<unknown>")) {
							((CtFieldAccess<?>) parent)
									.setTarget(factory.createTypeAccess(field.getDeclaringType(), false));
						}
					} else {
						((CtFieldAccess<?>) parent).setVariable(var.getReference());
						if (parent.hasParent(var.getDeclaringType()) && !var.isStatic()) {
							((CtFieldAccess<?>) parent).setTarget(
									factory.createThisAccess(parent.getParent(CtType.class).getReference(), true));
						} else {
							((CtFieldAccess<?>) parent)
									.setTarget(factory.createTypeAccess(var.getReference().getDeclaringType(), true));
							// ((CtFieldAccess<?>) parent).getTarget().setImplicit(true);
							// ((CtElement) ((CtFieldAccess<?>) parent).getTarget()).setImplicit(true);
						}
					}
				} else if (parent instanceof CtSuperAccess) {
				} else if (parent instanceof CtVariableAccess) {
					CtVariableReference ref = factory.createLocalVariableReference();
					ref.setSimpleName(target.getLabel());
					((CtVariableAccess<?>) parent).setVariable(ref);
				} else if (parent instanceof CtConstructorCall) {
					CtConstructorCall sp = getSpoonEle(source.getParent());
					CtExecutableReference ref = factory.Executable()
							.createReference(sp.getExecutable().getType().getQualifiedName() + " "
									+ sp.getExecutable().getSignature().replace("(", "#<init>("));
					((CtConstructorCall<?>) parent).setExecutable(ref);
				} else if (parent instanceof CtInvocation) {
					if (!target.getLabel().equals("<init>")) {
						CtExecutableReference ref = factory.createExecutableReference();
						ref.setSimpleName(target.getLabel());
						((CtInvocation<?>) parent).setExecutable(ref);
					}
				} else if (parent instanceof CtTypeAccess) {
					CtTypeAccess sp = getSpoonEle(source.getParent());
					CtTypeReference ref = factory.Type().createReference(sp.getAccessedType().getQualifiedName());
					if (ref.getPackage() != null)
						ref.getPackage().setImplicit(true);
					if (ref.getDeclaringType() != null)
						ref.getDeclaringType().setImplicit(true);
					ref.setSimpleName(target.getLabel());
					ref.setImplicit(((CtTypeAccess<?>) parent).isImplicit());
					((CtTypeAccess<?>) parent).setAccessedType(ref);
					CtExpression parentparent = (CtExpression) parent.getParent();
					// if (parentparent instanceof CtTargetedExpression ){
					if (target.getParent().getLabel().equals("TARGET")
							&& target.getParent().getParent().getLabel().equals("TARGET")) {
						((CtTargetedExpression) parentparent).setTarget((CtTypeAccess<?>) parent);
					}
				} else if (parent instanceof CtAnnotation) {
					((CtAnnotation) parent).setAnnotationType(factory.Type().createReference(target.getLabel()));
				} else if (parent instanceof CtThisAccess) { // TODO shouldn't get up to there
					// CtThisAccess ref = factory.createThisAccess();
					// ref.setSimpleName(target.getLabel());
					// ref.setImplicit(((CtTypeAccess<?>) parent).isImplicit());
					// ((CtThisAccess<?>) parent).setAccessedType(ref);
				} else if (parent instanceof CtOperatorAssignment) {
					((CtOperatorAssignment) parent).setKind(BinaryOperatorKind.valueOf(target.getLabel()));
				} else if (parent instanceof CtAssignment) { // TODO shouldn't get up to there
				} else if (parent instanceof CtReturn) { // TODO shouldn't get up to there
					// CtFieldWrite w = factory.createFieldWrite();
					// CtFieldReference v = factory.createFieldReference();
					// v.setSimpleName(target.getLabel());
					// w.setVariable(v);
					// ((CtAssignment) parent).setAssigned(w);

					// ((CtAssignment) parent).setLabel(target.getLabel());
				} else if (parent instanceof CtPackageReference) {
					// parent.replace(factory.Package().getOrCreate(target.getLabel()).getReference());
					((CtPackageReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtExecutableReference) {
					((CtExecutableReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtParameterReference) {
					((CtParameterReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtLocalVariableReference) {
					((CtLocalVariableReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtFieldReference) {
					((CtFieldReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtCatchVariableReference) {
					((CtCatchVariableReference) parent).setSimpleName(target.getLabel());
				} else {
					throw new UnsupportedOperationException(parent.getClass() + " for label");
				}
				break;
			}
			case "Interface": {
				CtInterface<?> created = factory.createInterface();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent == null) {
					factory.getModel().getRootPackage().addType(created);
				} else if (parent instanceof CtPackage) {
					created.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
					((CtPackage) parent).addType(created);
				} else {
					((CtType) parent).addNestedType(created);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Class": {
				CtClass<?> created = factory.createClass();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				// TODO eval if should add this new type to cu
				if (parent == null) {
					factory.getModel().getRootPackage().addType(created);
				} else if (parent instanceof CtPackage) {
					created.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
					((CtPackage) parent).addType(created);
				} else if (parent instanceof CtType) {
					created.setSimpleName("PlaceHolder" + ((CtType) parent).getNestedTypes().size());
					((CtType) parent).addNestedType(created);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else {
					((CtNewClass<?>) parent).setAnonymousClass(created);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "RootPackage": {
				CtPackage created = factory.getModel().getRootPackage();
				CtElement sp = getSpoonEle(source);
				// CtElement parent = getSpoonEleStrict(parentTarget);
				// created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(),parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Package": {
				CtPackage created;
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				if (parent == null || parent instanceof CtRootPackage) {
					String name = "placeholderpack" + factory.getModel().getRootPackage().getPackages().size();
					created = factory.Package().getOrCreate(name);
				} else {
					String name = "placeholderpack" + ((CtPackage) parent).getPackages().size();
					created = factory.createPackage((CtPackage) parent, name);
				}
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Method": {
				CtMethod<Object> created = factory.createMethod();
				CtElement sp = getSpoonEle(source);
				CtType<?> parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setDefaultMethod(((CtMethod) sp).isDefaultMethod());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				parent.addMethod(created);
				created.setSimpleName("placeHolder" + parent.getMethods().size());
				if (created.isAbstract()) {
					break;
				}
				if (parent instanceof CtInterface && !created.isDefaultMethod()) {
					break;
				}
				created.setBody(factory.createBlock());
				break;
			}
			case "RETURN_TYPE": {
				CtTypeReference created = factory.createTypeReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				((CtTypedElement<?>) parent).setType(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "MODIFIER": {
				// CtWrapper<CtExtendedModifier> sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);

				// if (!sp.getValue().getKind().toString().equals(target.getLabel())) {
				// 	throw new RuntimeException("wrong modifier");
				// }
				ModifierKind tmodk = ModifierKind.valueOf(target.getLabel().toUpperCase());
				// if (!sp.getValue().isImplicit())
				((CtModifiable) parent).addModifier(tmodk);

				CtExtendedModifier newV = ((CtModifiable) parent).getExtendedModifiers().stream()
						.filter(x -> x.getKind().equals(tmodk)).findAny().orElse(null);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, new CtWrapper.CtModifierWrapper(newV, parent));

				if (parent instanceof CtMethod && ((CtMethod) parent).isStatic()
						&& ((CtMethod) parent).getBody() == null)
					((CtMethod) parent).setBody(factory.createBlock());
				if (parent instanceof CtMethod && ((CtMethod) parent).isAbstract()
						&& ((CtMethod) parent).getBody() != null)
					((CtMethod) parent).setBody(null);
				break;
			}
			case "Field": {
				CtField<?> created = factory.createField();
				CtElement sp = getSpoonEle(source);
				CtType<?> parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setSimpleName("placeHolder" + parent.getFields().size());
				parent.addField(created);
				break;
			}
			case "VARIABLE_TYPE": {
				CtTypeReference created = factory.createTypeReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				((CtTypedElement<?>) parent).setType(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Literal": {
				CtLiteral created = factory.createLiteral();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "BinaryOperator": {
				CtBinaryOperator created = factory.createBinaryOperator();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "UnaryOperator": {
				CtUnaryOperator created = factory.createUnaryOperator();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				// if (parent == null) {
				// } else if (parent instanceof CtField) {
				// ((CtField<?>) parent).setDefaultExpression(created);
				// } else if (parent instanceof CtReturn) {
				// ((CtReturn<?>) parent).setReturnedExpression(created);
				// } else if (parent instanceof CtBodyHolder) {
				// addInBody(factory, target, created, (CtBodyHolder) parent);
				// } else if (parent instanceof CtIf) {
				// ((CtIf) parent).setCondition(created);
				// } else {
				// throw new UnsupportedOperationException(
				// parent.getClass().toString() + " as a parent is no handled");
				// }
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "FieldRead": {
				CtFieldRead created = factory.createFieldRead();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				CtTypeReference t = factory.Type().createReference(parent.getParent(CtClass.class));
				CtTypeAccess ta = factory.createTypeAccess(t);
				t.setParent(ta);
				created.setTarget(ta);
				ta.setParent(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "VariableRead": {
				CtVariableRead created = factory.createVariableRead();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "FieldWrite": {
				CtElement parent = getSpoonEleStrict(parentTarget);
				// CtVariable<?> var = factory.Query().createQuery(parent)
				// .map(new PotentialVariableDeclarationFunction("simpleName")).first();
				CtFieldWrite created = factory.createFieldWrite();
				CtElement sp = getSpoonEle(source);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtTypeReference topClassRef = factory.Type()
						.createReference(parent.getParent(CtType.class).getTopLevelType());
				CtTypeAccess typeAcc = factory.createTypeAccess(topClassRef);
				topClassRef.setParent(typeAcc);
				created.setTarget(typeAcc);
				typeAcc.setParent(created);
				addExpressionToParent(parent, created, target.getLabel());
				// factory.createTypeAcc
				// CtTypeReference auxRef = factory.createTypeReference();
				// auxRef.setSimpleName(parent.); // TODO need an helper to seach first parent
				// that contains this type
				// // with an implicit target I shound not need to specify it
				// CtTypeAccess defaultRef = factory.createTypeAccess(auxRef, true);
				// created.setTarget(defaultRef);
				break;
			}
			case "TypeAccess": {
				CtTypeAccess created = factory.createTypeAccess();
				CtElement parent = getSpoonEleStrict(parentTarget);
				CtElement sp = getSpoonEle(source);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setImplicit(((CtTypeAccess) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "AnonymousExecutable": {
				CtAnonymousExecutable created = factory.createAnonymousExecutable();
				CtElement sp = getSpoonEle(source);
				CtClass<?> parent = (CtClass<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				parent.addAnonymousExecutable(created);
				break;
			}
			case "Assignment": {
				CtAssignment created = factory.createAssignment();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				// if (parent.getBody() == null) {
				// parent.setBody(factory.createBlock());
				// }
				// int i = 0;
				// for (AbstractVersionedTree aaa : parentTarget.getAllChildren()) {
				// if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) == null) {
				// i++;
				// }
				// }
				// parent.getBody().addStatement(i, created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "OperatorAssignment": {
				CtOperatorAssignment created = factory.createOperatorAssignment();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setKind(BinaryOperatorKind.MINUS);

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				// if (parent.getBody() == null) {
				// parent.setBody(factory.createBlock());
				// }
				// int i = 0;
				// for (AbstractVersionedTree aaa : parentTarget.getAllChildren()) {
				// if (aaa.getMetadata("type").equals("MODIFIER")) {
				// continue;
				// }
				// if (aaa.getMetadata("type").equals("RETURN_TYPE")) {
				// continue;
				// }
				// if (getSpoonEle(aaa) != null) {
				// i++;
				// }
				// }
				// parent.getBody().addStatement(i, created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Return": {
				CtReturn created = factory.createReturn();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "SuperInvocation": {
				CtInvocation created = factory.createInvocation();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				// CtReturn<?> parent = (CtReturn<?>)
				// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtType parent2 = parent.getParent(CtType.class);
				CtTypeReference<?> superclassRef = parent2.getSuperclass();
				if (superclassRef == null) {
					superclassRef = factory.Type().OBJECT;
				}
				// CtClass superclass;
				// try {
				// 	superclass = (CtClass) factory.Class().get(superclassRef.getQualifiedName());
				// } catch (Exception e) {
				// 	throw new WrongAstContextException("cannot get the super Class", e);
				// }
				// if (superclass == null) {
				// 	CtPackage pack;
				// 	if (superclassRef.getPackage() != null) {
				// 		pack = factory.Package().get(superclassRef.getPackage().getQualifiedName());
				// 		if (pack == null) {
				// 			pack = factory.Package().getOrCreate(superclassRef.getPackage().getQualifiedName());
				// 			pack.setShadow(true);
				// 		}
				// 	} else {
				// 		pack = parent.getParent(CtPackage.class);
				// 	}
				// 	superclass = factory.createClass(pack, superclassRef.getSimpleName());
				// 	superclass.setShadow(true);
				// }
				// CtConstructor constructor = superclass.getConstructor();
				// if (constructor == null) {
				// 	constructor = factory.createConstructor(superclass, new HashSet<>(), new ArrayList<>(),
				// 			new HashSet<>(), factory.createBlock());
				// 	constructor.setShadow(true);
				// 	constructor.setImplicit(true);
				// 	if (!parent2.getReference().canAccess(constructor)) {
				// 		constructor.setVisibility(ModifierKind.PUBLIC);
				// 		superclass.setVisibility(ModifierKind.PUBLIC);
				// 	}
				// }
				// CtExecutableReference er = constructor.getReference();

				created.setExecutable(factory.Constructor().createReference(superclassRef));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ThisInvocation": {
				CtInvocation created = factory.createInvocation();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				CtExecutableReference er = factory.createExecutableReference();
				er.setDeclaringType(parent.getParent(CtType.class).getReference());
				er.setSimpleName("<init>");
				created.setExecutable(er);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Invocation": {
				CtInvocation created = factory.createInvocation();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				// CtReturn<?> parent = (CtReturn<?>)
				// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

				created.setTarget(factory.createThisAccess(
						(parent instanceof CtType ? (CtType) parent : parent.getParent(CtType.class)).getReference(),
						true));

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ThisAccess": {
				CtElement parent = getSpoonEleStrict(parentTarget);
				CtThisAccess created = factory.createThisAccess(parent.getParent(CtType.class).getReference(), true);
				CtElement sp = getSpoonEle(source);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setImplicit(((CtThisAccess) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "Parameter": {
				CtParameter<?> created = factory.createParameter();
				CtElement sp = getSpoonEle(source);
				CtExecutable<?> parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				parent.addParameter(created);
				break;
			}
			case "TypeReference": {
				CtTypeReference created = factory.createTypeReference();
				CtTypeReference sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				if (sp.getDeclaringType() != null) {
					CtTypeReference tref = factory.Type().createReference(sp.getDeclaringType().getQualifiedName());
					tref.setImplicit(true);
					tref.setParent(parent);
					created.setDeclaringType(tref);
				}
				if (sp.getPackage() != null) {
					CtPackageReference pref = factory.Package().createReference(sp.getPackage().getQualifiedName());
					pref.setImplicit(true);
					pref.setParent(parent);
					created.setPackage(pref);
				}
				if (target.getLabel().equals(CtRole.CAST.name())) {
					((CtExpression) parent).addTypeCast(created);
				} else if (target.getLabel().equals(CtRole.BOUNDING_TYPE.name())) {
					((CtWildcardReference) parent).setBoundingType(created);
				} else if (target.getLabel().equals(CtRole.SUPER_TYPE.name())) {
					((CtType) parent).setSuperclass(created);
				} else if (target.getLabel().equals(CtRole.INTERFACE.name())) {
					created.setSimpleName("PlaceHolder" + ((CtType) parent).getSuperInterfaces().size());
					((CtType) parent).addSuperInterface(created);
				} else if (target.getLabel().equals(CtRole.THROWN.name())) {
					((CtExecutable) parent).addThrownType(created);
				} else if (target.getLabel().equals(CtRole.DECLARING_TYPE.name())) {
					((CtTypeReference) parent).setDeclaringType(created);
				} else if (parent instanceof CtArrayTypeReference) {
					((CtArrayTypeReference) parent).setComponentType(created);
				} else if (parent instanceof CtTypeReference && target.getLabel().equals(CtRole.TYPE_ARGUMENT.name())) {
					((CtTypeReference) parent).addActualTypeArgument(created);
				} else if (parent instanceof CtIntersectionTypeReference && target.getLabel().equals(CtRole.BOUND.name())) {
					((CtIntersectionTypeReference) parent).addBound(created);
				} else if (parent instanceof CtTypeAccess) {
					((CtTypeAccess) parent).setAccessedType(created);
				} else if (parent instanceof CtField) {
					((CtField) parent).setType(created);
				} else if (parent instanceof CtExecutableReference) {
					((CtExecutableReference) parent).setType(created);
				} else if (parent instanceof CtAnnotation) {
					((CtAnnotation) parent).setAnnotationType(created);
				} else if (parent instanceof CtMethod) {
					((CtMethod) parent).setType(created);
				} else if (parent instanceof CtParameter) {
					((CtParameter) parent).setType(created);
				} else if (parent instanceof CtLocalVariable) {
					((CtLocalVariable) parent).setType(created);
				} else if (parent instanceof CtFieldReference) {
					((CtField) parent).setType(created);
				} else if (parent instanceof CtCatchVariable) {
					((CtCatchVariable) parent).setType(created);
				} else {
					throw new UnsupportedOperationException(
							parent.getClass().toString() + " as a parent is no handled for role " + target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			// case "SUPER_CLASS": {
			// CtType parent = (CtType)
			// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			// CtTypeReference sp = getSpoonEle(source);
			// CtTypeReference created =
			// factory.Type().createReference(sp.getQualifiedName());
			// parent.setSuperclass(created);
			// target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, (CtTypeReference)
			// created);
			// break;
			// }
			// TODO add generators for following elements
			case "Constructor": {
				CtConstructor cons = factory.createConstructor();
				CtElement sp = getSpoonEle(source);
				cons.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, cons);
				CtClass<?> parent = getSpoonEleStrict(parentTarget);
				parent.addConstructor(cons);
				cons.setSimpleName("placeHolder" + parent.getMethods().size());
				cons.setBody(factory.createBlock());
				break;
			}
			// case "INTERFACE": { // when inheriting interface
			// CtElement parent = getSpoonEleStrict(parentTarget);
			// CtType parentType = (CtType)
			// parentTarget.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			// parentType.addSuperInterface((CtTypeReference) parent);
			// target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, (CtTypeReference)
			// parent);
			// break;
			// }
			case "ConstructorCall": {
				CtConstructorCall created = factory.createConstructorCall();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				// CtReturn<?> parent = (CtReturn<?>)
				// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				// if (parent instanceof CtReturn) {
				// ((CtReturn<?>) parent).setReturnedExpression(created);
				// } else if (parent instanceof CtExecutable) {
				// addInBody(factory, target, (CtStatement) created, (CtExecutable<?>) parent);
				// } else if (parent instanceof CtRHSReceiver) {
				// ((CtRHSReceiver<?>) parent).setAssignment(created);
				// } else if (parent instanceof CtAbstractInvocation) {
				// ((CtAbstractInvocation<?>) parent).addArgument(created);
				// } else {
				// throw new UnsupportedOperationException(
				// parent.getClass().toString() + " as a parent is no handled");
				// }
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Try": {
				CtTry created = factory.createTry();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					throw new UnsupportedOperationException(parent.getClass().toString());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setBody(factory.createBlock());
				break;
			}
			case "TryWithResource": {
				CtTryWithResource created = factory.createTryWithResource();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					throw new UnsupportedOperationException(parent.getClass().toString());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setBody(factory.createBlock());
				break;
			}
			case "Catch": {
				CtCatch created = factory.createCatch();
				CtElement sp = getSpoonEle(source);
				CtTry parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				parent.addCatcher(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setBody(factory.createBlock());
				break;
			}
			case "If": {
				CtIf created = factory.createIf();
				CtIf sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				// if (sp.getThenStatement()!=null && sp.getThenStatement().isImplicit()) {
				// 	CtBlock created2 = factory.createBlock();
				// 	created2.setImplicit(sp.getThenStatement().isImplicit());
				// 	created.setThenStatement(created2);
				// }
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Case": {
				CtCase created = factory.createCase();
				CtElement sp = getSpoonEle(source);
				CtSwitch parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				parent.addCase(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "then": {
				CtBlock created = factory.createBlock();
				CtElement sp = getSpoonEle(source);
				CtIf parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				// created.setImplicit(sp.isImplicit());
				parent.setThenStatement(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Block": {
				CtBlock created = factory.createBlock();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setImplicit(sp.isImplicit());
				switch (target.getLabel()) {
					case "THEN": {
						((CtIf) parent).setThenStatement(created);
						break;
					}
					case "ELSE": {
						((CtIf) parent).setElseStatement(created);
						break;
					}
					case "STATEMENT": {
						if (parent instanceof CtBodyHolder) {
							addInBody(factory, target, created, (CtBodyHolder) parent);
						} else if (parent instanceof CtStatementList) {
							addInBody(factory, target, created, (CtStatementList) parent);
						} else if (parent instanceof CtSynchronized) {
							addInBody(factory, target, created, (CtSynchronized) parent);
						}
						break;
					}
					default:
						throw new UnsupportedOperationException(target.getLabel() + " role not handled");
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "else": {
				CtBlock created = factory.createBlock();
				CtElement sp = getSpoonEle(source);
				CtIf parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				parent.setElseStatement(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Conditional": {
				CtConditional created = factory.createConditional();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Switch": {
				CtSwitch created = factory.createSwitch();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "While": {
				CtWhile created = factory.createWhile();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "For": {
				CtFor created = factory.createFor();
				CtFor sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				if (((CtFor) sp).getBody() != null) {
					CtBlock<Object> block = factory.createBlock();
					block.setImplicit(sp.getBody().isImplicit());
					created.setBody(block);
				}
				break;
			}
			case "ForEach": {
				CtForEach created = factory.createForEach();
				CtForEach sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				if (((CtForEach) sp).getBody() != null) {
					CtBlock<Object> block = factory.createBlock();
					block.setImplicit(sp.getBody().isImplicit());
					created.setBody(block);
				}
				break;
			}
			case "LocalVariable": {
				CtLocalVariable<?> created = factory.createLocalVariable();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					created.setSimpleName("placeHolder" + (((CtBodyHolder) parent).getBody() == null ? 0
							: ((CtBlock) ((CtBodyHolder) parent).getBody()).getStatements().size()));
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					created.setSimpleName("placeHolder" + (((CtStatementList) parent) == null ? 0
							: ((CtStatementList) parent).getStatements().size()));
					addInBody(factory, target, created, (CtStatementList) parent);
				} else {
					created.setSimpleName("placeHolder" + (((CtSynchronized) parent).getBlock() == null ? 0
							: ((CtSynchronized) parent).getBlock().getStatements().size()));
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "NewArray": {
				CtNewArray created = factory.createNewArray();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				// CtReturn<?> parent = (CtReturn<?>)
				// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ArrayRead": {
				CtArrayRead created = factory.createArrayRead();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "ArrayWrite": {
				CtArrayWrite created = factory.createArrayWrite();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "VariableWrite": {
				CtVariableWrite created = factory.createVariableWrite();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "NewClass": {
				CtNewClass created = factory.createNewClass();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "CatchVariable": {
				CtCatchVariable created = factory.createCatchVariable();
				CtElement sp = getSpoonEle(source);
				CtCatch parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				parent.setParameter(created);
				break;
			}
			case "Break": {
				CtBreak created = factory.createBreak();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Continue": {
				CtContinue created = factory.createContinue();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "ArrayTypeReference": {
				CtArrayTypeReference created = factory.createArrayTypeReference();
				CtElement parent = getSpoonEleStrict(parentTarget);
				if (parent instanceof CtNewArray) {
					((CtNewArray) parent).setType(created);
				} else if (parent instanceof CtTypeReference) {
					((CtTypeReference) parent).addActualTypeArgument(created);
				} else {
					((CtTypedElement) parent).setType(created);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Throw": {
				CtThrow created = factory.createThrow();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Assert": {
				CtAssert created = factory.createAssert();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Do": {
				CtDo created = factory.createDo();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "THROWS": {
				CtTypeReference created = factory.createTypeReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				((CtMethod) parent).addThrownType(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "SuperAccess": {
				CtSuperAccess created = factory.createSuperAccess();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				CtTypeReference<?> pr = factory.Type().createReference(parent.getParent(CtType.class));
				CtVariableReference v = created.getVariable();
				pr.setParent(v);
				v.setType(pr);
				// factory.createVarTypeAccess(parent.getParent(CtType.class).getSuperclass());
				// created.setTarget();
				// created.setVariable(factory.createConstructorCall(parent.getParent(CtType.class).getSuperclass()));
				created.setImplicit(((CtSuperAccess) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "Annotation": {
				CtAnnotation created = factory.createAnnotation();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setImplicit(((CtAnnotation) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				if (parent instanceof CtBodyHolder) {
					((CtBodyHolder) parent).addAnnotation(created);
				} else if (parent instanceof CtStatementList) {
					((CtStatementList) parent).addAnnotation(created);
				} else if (parent instanceof CtType) {
					((CtType) parent).addAnnotation(created);
				} else if (parent instanceof CtExpression) {
					((CtExpression) parent).addAnnotation(created);
				} else if (parent instanceof CtStatement) {
					((CtStatement) parent).addAnnotation(created);
				} else if (parent instanceof CtTypeMember) {
					((CtTypeMember) parent).addAnnotation(created);
				} else {
					parent.addAnnotation(created);
				}
				break;
			}
			case "Synchronized": {
				CtSynchronized created = factory.createSynchronized();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setImplicit(((CtSynchronized) sp).isImplicit());
				if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else {
					throw new UnsupportedOperationException(parent.getClass().toString());
					// addExpressionToParent(parent, created, target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "WildcardReference": {
				CtWildcardReference created = factory.createWildcardReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setImplicit(((CtWildcardReference) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				if (parent instanceof CtTypeParameterReference) {
					((CtTypeParameterReference) parent).addActualTypeArgument(created);
				} else if (parent instanceof CtTypeReference) {
					((CtTypeReference) parent).addActualTypeArgument(created);
				} else {
					throw new UnsupportedOperationException(parent.getClass().toString());
				}
				break;
			}
			case "TypeParameter": {
				CtTypeParameter created = factory.createTypeParameter();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setImplicit(((CtTypeParameter) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				if (parent instanceof CtFormalTypeDeclarer) {
					((CtFormalTypeDeclarer) parent).addFormalCtTypeParameter(created);
				} else {
					throw new UnsupportedOperationException(parent.getClass().toString());
				}
				break;
			}
			case "Lambda": {
				CtLambda created = factory.createLambda();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "TypeParameterReference": {
				CtTypeParameterReference created = factory.createTypeParameterReference();
				CtElement parent = getSpoonEleStrict(parentTarget);
				if (target.getLabel().equals(CtRole.CAST.name())) {
					((CtExpression) parent).addTypeCast(created);
				} else if (target.getLabel().equals(CtRole.SUPER_TYPE.name())) {
					((CtType) parent).setSuperclass(created);
				} else if (target.getLabel().equals(CtRole.INTERFACE.name())) {
					((CtType) parent).addSuperInterface(created);
				} else if (parent instanceof CtArrayTypeReference) {
					((CtArrayTypeReference) parent).setComponentType(created);
				} else if (parent instanceof CtTypeReference) {
					((CtTypeReference) parent).addActualTypeArgument(created);
				} else if (target.getLabel().equals(CtRole.TYPE.name())) {
					((CtTypedElement) parent).setType(created);
				} else if (target.getLabel().equals(CtRole.THROWN.name())) {
					((CtExecutable) parent).addThrownType(created);
				} else if (target.getLabel().equals(CtRole.TYPE_ARGUMENT.name()) && parent instanceof CtExecutableReference) {
					((CtExecutableReference) parent).addActualTypeArgument(created);
				} else {
					throw new UnsupportedOperationException(
							parent.getClass().toString() + " as a parent is no handled for role " + target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ExecutableReferenceExpression": {
				CtExecutableReferenceExpression created = factory.createExecutableReferenceExpression();
				CtElement parent = getSpoonEleStrict(parentTarget);
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Enum": {
				CtEnum created = factory.createEnum();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent == null) {
					factory.getModel().getRootPackage().addType(created);
				} else if (parent instanceof CtPackage) {
					created.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
					((CtPackage) parent).addType(created);
				} else if (parent instanceof CtType) {
					created.setSimpleName("PlaceHolder" + ((CtType) parent).getNestedTypes().size());
					((CtType) parent).addNestedType(created);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtSynchronized) {
					addInBody(factory, target, created, (CtSynchronized) parent);
				} else if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else {
					((CtNewClass<?>) parent).setAnonymousClass(created);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "EnumValue": {
				CtEnumValue created = factory.createEnumValue();
				CtElement sp = getSpoonEle(source);
				CtEnum<?> parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setSimpleName("placeHolder" + parent.getFields().size());
				parent.addEnumValue(created);
				break;
			}
			case "IntersectionTypeReference": {
				CtIntersectionTypeReference created = factory.createIntersectionTypeReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (target.getLabel().equals(CtRole.CAST.name())) {
					((CtExpression) parent).addTypeCast(created);
				} else if (target.getLabel().equals(CtRole.SUPER_TYPE.name())) {
					((CtType) parent).setSuperclass(created);
				} else if (target.getLabel().equals(CtRole.INTERFACE.name())) {
					created.setSimpleName("PlaceHolder" + ((CtType) parent).getSuperInterfaces().size());
					((CtType) parent).addSuperInterface(created);
				} else if (parent instanceof CtArrayTypeReference) {
					((CtArrayTypeReference) parent).setComponentType(created);
				} else if (parent instanceof CtTypeReference) {
					((CtTypeReference) parent).addActualTypeArgument(created);
				} else {
					throw new UnsupportedOperationException(
							parent.getClass().toString() + " as a parent is no handled for role " + target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "PackageReference": {
				CtPackageReference created = factory.createPackageReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				// created.setSimpleName("placeHolder" + parent.getFields().size());
				if (parent instanceof CtAnnotation) {
					CtTypeReference aaa = ((CtAnnotation) parent).getAnnotationType();
					aaa.setPackage(created);
					created.setParent(aaa);
					((CtAnnotation) parent).setAnnotationType(aaa);
				} else if (parent instanceof CtTypeReference) {
					((CtTypeReference) parent).setPackage(created);
					created.setParent(parent);
					CtElement parentparent = parent.getParent();
					if (parent.getRoleInParent().equals(CtRole.THROWN)) {
						((CtExecutable) parentparent).addThrownType((CtTypeReference) parent);
					} else if (parentparent instanceof CtTypedElement) {// CtField
						((CtTypedElement) parentparent).setType((CtTypeReference) parent);
					} else if (parentparent instanceof CtTypeReference) {
						parentparent.isImplicit();
						// ((CtTypeReference)parentparent).setAType((CtTypeReference)parent);
					} else if (parentparent instanceof CtExecutableReference) {
						((CtExecutableReference) parentparent).setType((CtTypeReference) parent);
					} else if (parentparent instanceof CtTypeParameter) {
						if (parent.getRoleInParent().equals(CtRole.SUPER_TYPE)) {
							((CtTypeParameter) parentparent).setSuperclass((CtTypeReference) parent);
						} else {
							throw new RuntimeException(parentparent.getClass().toString() + " with role " + parent.getRoleInParent().toString());
						}
					} else if (parentparent instanceof CtType) {
						// nothing to do
					} else {
						throw new RuntimeException(parentparent.getClass().toString() + " with role " + parent.getRoleInParent().toString());
					}
				} else if (parent instanceof CtExecutableReference) {
					((CtExecutableReference) parent).isImplicit();// TODO
					break;
				} else if (parent instanceof CtNewArray) {
					((CtNewArray) parent).isImplicit();// TODO
					break;
				} else if (parent instanceof CtClass) {
					LOGGER.warning("this AST structure should not materialized");
					break;
				} else {
					throw new RuntimeException(parent.getClass().toString() + " with role " + target.getLabel());
				}

				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ExecutableReference": {
				CtExecutableReference created = factory.createExecutableReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parentTarget.getMetadata("type").equals("SuperInvocation")) {
				} else if (parentTarget.getMetadata("type").equals("ThisInvocation")) {
				} else if (parent instanceof CtAbstractInvocation) {
					((CtAbstractInvocation) parent).setExecutable(created);
				} else if (parent instanceof CtExecutableReferenceExpression) {
					((CtExecutableReferenceExpression) parent).setExecutable(created);
				} else {
					throw new RuntimeException(parent.getClass().toString());
				}

				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ParameterReference": {
				CtParameterReference created = factory.createParameterReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtVariableAccess) {
					((CtVariableAccess) parent).setVariable(created);
				} else {
					throw new RuntimeException(parent.getClass().toString());
				}

				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "LocalVariableReference": {
				CtLocalVariableReference created = factory.createLocalVariableReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtVariableAccess) {
					((CtVariableAccess) parent).setVariable(created);
				} else {
					throw new RuntimeException(parent.getClass().toString());
				}

				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "FieldReference": {
				CtFieldReference created = factory.createFieldReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtVariableAccess) {
					((CtVariableAccess) parent).setVariable(created);
				} else {
					throw new RuntimeException(parent.getClass().toString());
				}

				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "CatchVariableReference": {
				CtCatchVariableReference created = factory.createCatchVariableReference();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent instanceof CtVariableAccess) {
					((CtVariableAccess) parent).setVariable(created);
				} else {
					throw new RuntimeException(parent.getClass().toString());
				}

				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "AnnotationMethod": {
				CtAnnotationMethod created = factory.createAnnotationMethod();
				CtElement sp = getSpoonEle(source);
				CtAnnotationType parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				created.setDefaultMethod(((CtMethod) sp).isDefaultMethod());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				parent.addMethod(created);
				created.setSimpleName("placeHolder" + parent.getMethods().size());
				if (created.isAbstract()) {
					break;
				}
				if (parent instanceof CtInterface && !created.isDefaultMethod()) {
					break;
				}
				created.setBody(factory.createBlock());
				break;
			}
			case "AnnotationType": {
				CtAnnotationType created = factory.createAnnotationType();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				if (parent == null) {
					factory.getModel().getRootPackage().addType(created);
				} else if (parent instanceof CtPackage) {
					created.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
					((CtPackage) parent).addType(created);
				} else {
					((CtType) parent).addNestedType(created);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "AnnotationFieldAccess": {
				CtAnnotationFieldAccess created = factory.createAnnotationFieldAccess();
				CtElement sp = getSpoonEle(source);
				CtElement parent = getSpoonEleStrict(parentTarget);
				created.setPosition(new MyOtherCloner(factory).clone(sp.getPosition(), parent));
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			default: {
				LOGGER.warning(targetType + " is no handled");
				throw new AssertionError(targetType + " is no handled");
				// throw new UnsupportedOperationException(targetType + " is no handled");
			}
		}
		target.setMetadata("inserted", true);
	}

	private static <T extends CtElement> T getSpoonEle(ITree tree) {
		T r;
		r = (T) tree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		if (r == null) {
			r = (T) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
		}
		return r;
	}

	private static <T extends CtElement> T getSpoonEleStrict(ITree tree) throws MissingParentException {
		T r;
		r = (T) tree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		if (r == null) {
			String treeString = tree.toTreeString();
			List<ITree> parents = tree.getParents();
			throw new MissingParentException("node " + tree.toString() + 
				" with parents " + parents.toString() + " should contain a spoon object" + 
				" considering i=" + Objects.toString(tree.getMetadata("inserted")) +
				", d=" + Objects.toString(tree.getMetadata("deleted")) +
				", u=" + Objects.toString(tree.getMetadata("updated")) + 
				", -u=" + Objects.toString(tree.getMetadata("unupdated")));
		}
		return r;
	}

	// private static void addExpressionToParent(CtElement parent, CtExpression
	// created) {
	// addExpressionToParent(parent, created, null);
	// }

	private static void addExpressionToParent(CtElement parent, CtExpression created, String role) {
		if (parent == null) {
			throw new UnsupportedOperationException("parent is null");
		} else if (parent instanceof CtField) {
			((CtField<?>) parent).setDefaultExpression(created);
		} else if (parent instanceof CtReturn) {
			((CtReturn<?>) parent).setReturnedExpression(created);
		} else if (role.equals(CtRole.TARGET.name())) {
			((CtTargetedExpression) parent).setTarget(created);
		} else if (parent instanceof CtArrayAccess) {
			((CtArrayAccess) parent).setIndexExpression(created);
		} else if (role.equals(CtRole.DIMENSION.name())) {
			((CtNewArray) parent).addDimensionExpression(created);
		} else if (parent instanceof CtAbstractInvocation) {
			((CtAbstractInvocation<?>) parent).addArgument(created);
		} else if (parent instanceof CtAssert) {
			((CtAssert) parent).setExpression(created);
		} else if (parent instanceof CtIf) {
			((CtIf) parent).setCondition(created);
		} else if (parent instanceof CtFor) {
			((CtFor) parent).setExpression(created);
		} else if (parent instanceof CtForEach) {
			((CtForEach) parent).setExpression(created);
		} else if (parent instanceof CtWhile) {
			((CtWhile) parent).setLoopingExpression(created);
		} else if (parent instanceof CtDo) {
			((CtDo) parent).setLoopingExpression(created);
		} else if (parent instanceof CtSwitch) {
			((CtSwitch) parent).setSelector(created);
		} else if (parent instanceof CtConditional) {
			if (role.equals(CtRole.CONDITION.name())) {
				((CtConditional) parent).setCondition(created);
			} else if (role.equals(CtRole.THEN.name())) {
				((CtConditional) parent).setThenExpression(created);
			} else if (role.equals(CtRole.ELSE.name())) {
				((CtConditional) parent).setElseExpression(created);
			} else {
				throw new UnsupportedOperationException(
						role + " role not supported for " + parent.getClass().toString());
			}
		} else if (parent instanceof CtLocalVariable) {
			((CtLocalVariable) parent).setDefaultExpression(created);
		} else if (parent instanceof CtSynchronized) {
			((CtSynchronized) parent).setExpression(created);
		} else if (parent instanceof CtRHSReceiver) {
			if (role.equals(CtRole.ASSIGNED.name())) {
				((CtAssignment<?, ?>) parent).setAssigned(created);
			} else if (role.equals(CtRole.ASSIGNMENT.name())) {
				((CtAssignment<?, ?>) parent).setAssignment(created);
			} else {
				throw new UnsupportedOperationException(
						role + " role not supported for " + parent.getClass().toString());
			}
		} else if (parent instanceof CtUnaryOperator) {
			((CtUnaryOperator<?>) parent).setOperand(created);
		} else if (parent instanceof CtCase) {
			((CtCase) parent).setCaseExpression(created);
		} else if (parent instanceof CtAssignment) {
			((CtAssignment<?, ?>) parent).setAssignment(created);
		} else if (parent instanceof CtBinaryOperator || parent instanceof CtAssignment) {
			if (role.equals(CtRole.LEFT_OPERAND.name())) {
				((CtBinaryOperator<?>) parent).setLeftHandOperand(created);
			} else if (role.equals(CtRole.RIGHT_OPERAND.name())) {
				((CtBinaryOperator<?>) parent).setRightHandOperand(created);
			} else {
				throw new UnsupportedOperationException(
						role + " role not supported for " + parent.getClass().toString());
			}
		} else if (parent instanceof CtThrow) {
			((CtThrow) parent).setThrownExpression(created);
		} else if (parent instanceof CtNewArray) {
			((CtNewArray) parent).addDimensionExpression(created);
		} else if (parent instanceof CtSynchronized) {
			((CtSynchronized) parent).setExpression(created);
		} else if (parent instanceof CtField) {
			((CtField) parent).setDefaultExpression(created);
		} else if (parent instanceof CtAnnotation) {
			((CtAnnotation) parent).addValue("value", created);
		} else if (parent instanceof CtLambda) {
			((CtLambda) parent).setExpression(created);
		} else if (parent instanceof CtAnnotationMethod) {
			((CtAnnotationMethod) parent).setDefaultExpression(created);
		} else {
			throw new UnsupportedOperationException(
					parent.getClass().toString() + " as a parent is no handled for " + created.getClass().toString());
		}
	}

	static void addInBody(Factory factory, AbstractVersionedTree target, CtStatement created, CtBodyHolder parent) {
		if (target.getLabel().equals(CtRole.FOR_INIT.name())) {
			((CtFor) parent).addForInit(created);
			return;
		} else if (target.getLabel().equals(CtRole.FOREACH_VARIABLE.name())) {
			((CtForEach) parent).setVariable((CtLocalVariable) created);
			return;
		} else if (target.getLabel().equals(CtRole.FOR_UPDATE.name())) {
			((CtFor) parent).addForUpdate(created);
			return;
		} else if (target.getLabel().equals(CtRole.TRY_RESOURCE.name())) {
			((CtTryWithResource) parent).addResource((CtLocalVariable)created);
			return;
		} else if (target.getLabel().equals(CtRole.EXPRESSION.name())) {
			if (parent instanceof CtFor) {
				((CtFor) parent).setExpression((CtExpression) created);
			} else if (parent instanceof CtForEach) {
				((CtForEach) parent).setExpression((CtExpression) created);
			} else if (parent instanceof CtDo) {
				((CtDo) parent).setLoopingExpression((CtExpression) created);
			} else if (parent instanceof CtLambda) {
				((CtLambda) parent).setExpression((CtExpression) created);
			} else {
				((CtWhile) parent).setLoopingExpression((CtExpression) created);
			}
			return;
		}
		if (parent.getBody() == null) {
			parent.setBody(factory.createBlock());
		}
		int i = 0;
		for (AbstractVersionedTree aaa : target.getParent().getAllChildren()) {
			if (shouldIgnore1(aaa)) {
				continue;
			}
			if (shouldIgnore2(parent, aaa)) {
				continue;
			}
			// if (target.equals(aaa)) {
			// ((CtBlock) parent.getBody()).addStatement(i, created);
			// return;
			// }
			if (aaa == target)
				break;
			if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null) {
				i++;
			}
			// if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) == null ||
			// aaa.getMetadata("type").equals("LABEL")
			// || aaa.getMetadata("type").equals("MODIFIER") ||
			// aaa.getMetadata("type").equals("RETURN_TYPE")) {
			// continue;
			// }
			// i++;
		}
		((CtBlock) parent.getBody()).addStatement(i, created);
	}

	private static boolean shouldIgnore1(AbstractVersionedTree aaa) {
		return aaa.getMetadata("type").equals("MODIFIER") || aaa.getMetadata("type").equals("RETURN_TYPE")
				|| aaa.getMetadata("type").equals("THROWS") || aaa.getLabel().equals("THROWN")
				|| aaa.getMetadata("type").equals("TypeParameter") || aaa.getMetadata("type").equals("Catch")
				|| aaa.getMetadata("type").equals("LABEL") || aaa.getMetadata("type").equals("Parameter")
				|| aaa.getMetadata("type").equals("Annotation") || aaa.getLabel().equals("EXPRESSION")
				|| aaa.getMetadata("type").equals("TypeReference") || aaa.getLabel().equals("TYPE");
	}

	private static boolean shouldIgnore2(CtBodyHolder parent, AbstractVersionedTree aaa) {
		return (parent instanceof CtCatch && aaa.getLabel().equals(CtRole.PARAMETER.name()))
				|| (parent instanceof CtFor && aaa.getLabel().equals(CtRole.FOR_UPDATE.name()))
				|| (parent instanceof CtForEach && aaa.getLabel().equals(CtRole.FOREACH_VARIABLE.name()))
				|| (parent instanceof CtFor && aaa.getLabel().equals(CtRole.FOR_INIT.name()))
				|| (parent instanceof CtFor && aaa.getLabel().equals(CtRole.EXPRESSION.name()))
				|| (parent instanceof CtWhile && aaa.getLabel().equals(CtRole.EXPRESSION.name()))
				|| (parent instanceof CtDo && aaa.getLabel().equals(CtRole.EXPRESSION.name()))
				|| (parent instanceof CtTryWithResource && aaa.getLabel().equals(CtRole.TRY_RESOURCE.name()))
				|| (parent instanceof CtExecutableReference && aaa.getLabel().equals(CtRole.TYPE_ARGUMENT.name()));
	}

	static void addInBody(Factory factory, AbstractVersionedTree target, CtStatement created, CtStatementList parent) {
		int i = 0;
		for (AbstractVersionedTree aaa : target.getParent().getAllChildren()) {
			if (shouldIgnore1(aaa)) {
				continue;
			}
			// if (target.equals(aaa)) {
			// parent.addStatement(i, created);
			// return;
			// }
			if (aaa == target)
				break;
			if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null) {
				i++;
			}
		}
		parent.addStatement(i, created);
	}

	static void addInBody(Factory factory, AbstractVersionedTree target, CtStatement created, CtSynchronized parent) {
		if (parent.getBlock() == null) {
			parent.setBlock(factory.createBlock());
		}
		addInBody(factory, target, created, parent.getBlock());
	}

	public static void applyMyDelete(Factory factory, TreeContext ctx, MyDelete action)
			throws WrongAstContextException {
		// ITree source = action.getSource();
		AbstractVersionedTree target = action.getTarget();
		AbstractVersionedTree parentTarget = target.getParent();
		CtElement ele = (CtElement) target.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		target.setMetadata("deleted", false);
		if (ele != null) {
			try {
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, null);
				if (ele instanceof CtWrapper && ((CtWrapper) ele).getValue() instanceof CtExtendedModifier) {
					CtModifiable parent = getSpoonEleStrict(parentTarget);
					parent.removeModifier(((CtWrapper<CtExtendedModifier>) ele).getValue().getKind());
					// TODO maybe better to correctly impl CtWrapper.delete()
				} else {
					ele.delete();
				}
			} catch (Exception e) {
				throw new WrongAstContextException("while deleting", e);
			}
		}
		target.setMetadata("deleted", true);
	}

	public static void applyMyUpdate(Factory factory, TreeContext ctx, MyUpdate action) throws MissingParentException {
		AbstractVersionedTree target = action.getTarget();
		AbstractVersionedTree parentTarget = target.getParent();
		String targetType = (String) target.getMetadata("type");
		// action.getEditScript().getRightVersion();
		ITree source = action.getNode();
		// ITree source = action.getSource();
		// TODO find previously instanciated thing to uninstantiat it after instanciating target
		target.setMetadata("updated", false);
		source.setMetadata("unupdated", false);
		switch (targetType) {
			case "LABEL": {
				CtElement parent = getSpoonEleStrict(parentTarget);
				if (parent == null) {
					LOGGER.warning("no parent for label " + target.getLabel());
				} else if (parent instanceof CtPackage) {
					String toCompAgainst = ((CtPackage) parent).getQualifiedName();
					List<CtPackageReference> pRefToChange = new ArrayList<>();
					for (CtType<?> c : factory.getModel().getAllTypes()) {
						pRefToChange.addAll(c.getElements(new Filter<CtPackageReference>() {
							@Override
							public boolean matches(CtPackageReference x) {
								return x.isImplicit() && x.getQualifiedName().startsWith(toCompAgainst);
							}
						}));
					}
					((CtNamedElement) parent).setSimpleName(target.getLabel());
					String newQPName = ((CtPackage) parent).getQualifiedName();
					for (CtPackageReference p : pRefToChange) {
						if (p.getQualifiedName().startsWith(toCompAgainst)) {
							p.setSimpleName(newQPName + p.getSimpleName().substring(toCompAgainst.length()));
						}
					}
					// TODO update all refs to old package --'
				} else if (parent instanceof CtNamedElement) {
					((CtNamedElement) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtWildcardReference) {
				} else if (parent instanceof CtTypeReference) {
					// if (!parent.isParentInitialized()) {
					// 	throw null;
					// } else if (CtRole.SUPER_TYPE.equals(parent.getRoleInParent())) {
					// 	((CtTypeReference)parent).setSimpleName(target.getLabel());
					// } else if (CtRole.INTERFACE.equals(parent.getRoleInParent())) {
					// 	((CtTypeReference)parent).setSimpleName(target.getLabel());
					// } else {
					// 	((CtTypeReference)parent).setSimpleName(target.getLabel());
					// }
					((CtTypeReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtBinaryOperator) {
					((CtBinaryOperator<?>) parent).setKind(BinaryOperatorKind.valueOf(target.getLabel()));
				} else if (parent instanceof CtUnaryOperator) {
					((CtUnaryOperator<?>) parent).setKind(UnaryOperatorKind.valueOf(target.getLabel()));
				} else if (parent instanceof CtLiteral) {
					if (target.getLabel().startsWith("\"")) {
						((CtLiteral<String>) parent).setValue(target.getLabel()
								.substring(1, target.getLabel().length() - 1).replace("\\\\", "\\").toString());
						((CtLiteral<String>) parent).setType(factory.Type().STRING);
						CtLiteral sl = getSpoonEle(source.getParent());
						int len = sl.getPosition().getSourceEnd() - sl.getPosition().getSourceStart();
						((CtLiteral<String>) parent).setPosition(new NoSourcePosition() {
							@Override
							public boolean isValidPosition() {
								return true;
							}

							@Override
							public int getSourceStart() {
								return 0;
							}

							@Override
							public int getSourceEnd() {
								return len;
							}
						});
					} else if (target.getLabel().startsWith("'")) {
						((CtLiteral<Character>) parent)
								.setValue(target.getLabel().substring(1, target.getLabel().length() - 1).charAt(0));
						((CtLiteral<Character>) parent).setType(factory.Type().CHARACTER_PRIMITIVE);
						CtLiteral sl = getSpoonEle(source.getParent());
						int len = sl.getPosition().getSourceEnd() - sl.getPosition().getSourceStart();
						((CtLiteral<String>) parent).setPosition(new NoSourcePosition() {
							@Override
							public boolean isValidPosition() {
								return true;
							}

							@Override
							public int getSourceStart() {
								return 0;
							}

							@Override
							public int getSourceEnd() {
								return len;
							}
						});
					} else if (target.getLabel().equals("true")) {
						((CtLiteral<Boolean>) parent).setValue(true);
					} else if (target.getLabel().equals("false")) {
						((CtLiteral<Boolean>) parent).setValue(false);
					} else if (target.getLabel().equals("null")) {
						((CtLiteral<Object>) parent).setValue(null);
					} else if (target.getLabel().endsWith("F")) {
						((CtLiteral<Float>) parent).setValue(
								Float.parseFloat(target.getLabel().substring(0, target.getLabel().length() - 1)));
					} else if (target.getLabel().endsWith("L")) {
						if (target.getLabel().startsWith("0x")) {
							((CtLiteral) parent).setBase(LiteralBase.HEXADECIMAL);
							((CtLiteral<Long>) parent).setValue(
									Long.decode(target.getLabel().substring(0, target.getLabel().length() - 1)));
						} else {
							((CtLiteral<Long>) parent).setValue(
									Long.parseLong(target.getLabel().substring(0, target.getLabel().length() - 1)));
						}
					} else if (target.getLabel().endsWith("D")) {
						((CtLiteral<Double>) parent).setValue(
								Double.parseDouble(target.getLabel().substring(0, target.getLabel().length())));
					} else {
						if (target.getLabel().startsWith("0x")) {
							((CtLiteral) parent).setBase(LiteralBase.HEXADECIMAL);
						}
						try {
							((CtLiteral<Integer>) parent).setValue(Integer.decode(target.getLabel()));
						} catch (Exception e) {
							try {
								((CtLiteral<Long>) parent).setValue(Long.decode(target.getLabel()));
							} catch (Exception ee) {
								((CtLiteral<Double>) parent).setValue(Double.parseDouble(target.getLabel()));
							}
						}
					}
				} else if (parent instanceof CtFieldAccess) {
					CtField var;
					try {
						var = factory.Query().createQuery(parent)
								.map(new PotentialVariableDeclarationFunction(target.getLabel())).first(CtField.class);
					} catch (Exception e) {
						var = null;
					}

					// CtVariableReference ref = factory.createFieldReference();
					// ref.setSimpleName(target.getLabel());
					if (var == null) {
						CtFieldAccess sps = (CtFieldAccess) source.getParent()
								.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
						CtFieldReference field = sps.getVariable().getType() == null
								? factory.Field()
										.createReference(factory.Type().NULL_TYPE.toString() + " "
												+ sps.getVariable().getQualifiedName())
								: factory.Field().createReference(sps.getVariable().getType().getQualifiedName() + " "
										+ sps.getVariable().getQualifiedName());
						((CtFieldAccess<?>) parent).setVariable(field);
						((CtFieldAccess<?>) parent)
								.setTarget(factory.createTypeAccess(field.getDeclaringType(), false));
					} else {
						((CtFieldAccess<?>) parent).setVariable(var.getReference());
						if (parent.hasParent(var.getDeclaringType()) && !var.isStatic()) {
							((CtFieldAccess<?>) parent).setTarget(
									factory.createThisAccess(parent.getParent(CtType.class).getReference(), true));
						} else {
							((CtFieldAccess<?>) parent)
									.setTarget(factory.createTypeAccess(var.getReference().getDeclaringType(), true));
							// ((CtFieldAccess<?>) parent).getTarget().setImplicit(true);
							// ((CtElement) ((CtFieldAccess<?>) parent).getTarget()).setImplicit(true);
						}
					}
				} else if (parent instanceof CtSuperAccess) {
				} else if (parent instanceof CtVariableAccess) {
					CtVariableReference ref = factory.createLocalVariableReference();
					ref.setSimpleName(target.getLabel());
					((CtVariableAccess<?>) parent).setVariable(ref);
				} else if (parent instanceof CtConstructorCall) {
					CtConstructorCall sp = getSpoonEle(source.getParent());
					CtExecutableReference ref = factory.Executable()
							.createReference(sp.getExecutable().getType().getQualifiedName() + " "
									+ sp.getExecutable().getSignature().replace("(", "#<init>("));
					((CtConstructorCall<?>) parent).setExecutable(ref);
				} else if (parent instanceof CtInvocation) {
					if (!target.getLabel().equals("<init>")) {
						CtExecutableReference ref = factory.createExecutableReference(); // TODO maybe get the old one
																							// if available
						ref.setSimpleName(target.getLabel());
						((CtInvocation<?>) parent).setExecutable(ref);
					}
				} else if (parent instanceof CtTypeAccess) {
					CtTypeAccess sp = getSpoonEle(source.getParent());
					CtTypeReference ref = factory.Type().createReference(sp.getAccessedType().getQualifiedName());
					if (ref.getPackage() != null)
						ref.getPackage().setImplicit(true);
					if (ref.getDeclaringType() != null)
						ref.getDeclaringType().setImplicit(true);
					ref.setSimpleName(target.getLabel());
					ref.setImplicit(((CtTypeAccess<?>) parent).isImplicit());
					((CtTypeAccess<?>) parent).setAccessedType(ref);
					CtExpression parentparent = (CtExpression) parent.getParent();
					// if (parentparent instanceof CtTargetedExpression ){
					if (target.getParent().getLabel().equals("TARGET")
							&& target.getParent().getParent().getLabel().equals("TARGET")) {
						((CtTargetedExpression) parentparent).setTarget((CtTypeAccess<?>) parent);
					}
				} else if (parent instanceof CtAnnotation) {
					((CtAnnotation) parent).setAnnotationType(factory.Type().createReference(target.getLabel()));
				} else if (parent instanceof CtThisAccess) { // TODO shouldn't get up to there
					// CtThisAccess ref = factory.createThisAccess();
					// ref.setSimpleName(target.getLabel());
					// ref.setImplicit(((CtTypeAccess<?>) parent).isImplicit());
					// ((CtThisAccess<?>) parent).setAccessedType(ref);
				} else if (parent instanceof CtOperatorAssignment) {
					((CtOperatorAssignment) parent).setKind(BinaryOperatorKind.valueOf(target.getLabel()));
				} else if (parent instanceof CtAssignment) { // TODO shouldn't get up to there
				} else if (parent instanceof CtReturn) { // TODO shouldn't get up to there
					// CtFieldWrite w = factory.createFieldWrite();
					// CtFieldReference v = factory.createFieldReference();
					// v.setSimpleName(target.getLabel());
					// w.setVariable(v);
					// ((CtAssignment) parent).setAssigned(w);

					// ((CtAssignment) parent).setLabel(target.getLabel());
				} else if (parent instanceof CtPackageReference) {
					// if (parent.isParentInitialized()){
					// 	CtElement parentparent = parent.getParent();
					// 	if (parentparent.isParentInitialized() && CtRole.SUPER_TYPE.equals(parentparent.getRoleInParent())) {
					// 		assert parentparent instanceof CtTypeReference: parentparent.getClass();
					// 		((CtPackageReference)parent).setSimpleName(factory.Package().getOrCreate(target.getLabel()).getReference().getSimpleName());
					// 		// ((CtType)parentparent.getParent()).setSuperclass((CtTypeReference)parentparent);
					// 	} else if (parentparent.isParentInitialized() && CtRole.INTERFACE.equals(parentparent.getRoleInParent())) {
					// 		assert parentparent instanceof CtTypeReference: parentparent.getClass();
					// 		((CtPackageReference)parent).setSimpleName(factory.Package().getOrCreate(target.getLabel()).getReference().getSimpleName());
					// 		// ((CtType)parentparent.getParent()).setSuperclass((CtTypeReference)parentparent);
					// 	} else {
					// 		throw null;
					// 	}
					// }
					((CtPackageReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtExecutableReference) {
					((CtExecutableReference) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtVariableReference) {
					((CtVariableReference) parent).setSimpleName(target.getLabel());
				} else {
					throw new UnsupportedOperationException(parent.getClass() + " for label");
				}
				break;
			}
			case "MODIFIER": {

				CtWrapper<CtExtendedModifier> smod = getSpoonEle(source);
				ModifierKind smodk = ModifierKind.valueOf(source.getLabel().toUpperCase());
				CtWrapper<CtExtendedModifier> tmod = getSpoonEle(target);
				ModifierKind tmodk = ModifierKind.valueOf(target.getLabel().toUpperCase());
				CtModifiable parent = getSpoonEleStrict(parentTarget);
				// if (!tmod.getValue().getKind().toString().equals(target.getLabel())) {
				// 	throw new RuntimeException("wrong modifier");
				// }
				// CtExtendedModifier actualSmod = ((CtModifiable) getSpoonEle(target.getParent())).getExtendedModifiers().stream()
				// 		.filter(x -> x.getKind().equals(smodk)).findFirst().orElse(null);
				// CtExtendedModifier actualTmod = ((CtModifiable) getSpoonEle(target.getParent())).getExtendedModifiers().stream()
				// 		.filter(x -> x.getKind().equals(tmodk)).findFirst().orElse(null);

				parent.removeModifier(smodk);
				parent.addModifier(tmodk);

				// if (actualTmod == null) {
				// } else {
				// 	actualTmod.setImplicit(false);
				// }
				source.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, null);
				source.setMetadata("unupdated", true);
				CtExtendedModifier newV = ((CtModifiable) parent).getExtendedModifiers().stream()
						.filter(x -> x.getKind().equals(tmodk)).findAny().orElse(null);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, newV);
				break;
			}
			case "VariableRead":
			case "FieldRead":
			case "BinaryOperator":
			case "UnaryOperator":
			case "ConstructorCall":
			case "Invocation":
			case "TypeReference":
			case "VARIABLE_TYPE":
			case "STATEMENT":
			case "Block":
			case "RETURN_TYPE":
				CtElement old = getSpoonEleStrict(source);
				source.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, null);
				source.setMetadata("unupdated", true);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, old);
				break;
			default:
				LOGGER.warning(targetType + " is no handled");
		}
		source.setMetadata("unupdated", true);
		target.setMetadata("updated", true);
	}

	// public static <T extends Move & AAction<Move>> void applyMyMove(Factory factory, TreeContext ctx, T action) {
	// 	throw new UnsupportedOperationException();
	// }

}