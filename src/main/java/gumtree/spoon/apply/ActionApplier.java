package gumtree.spoon.apply;

import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.compiler.Environment;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtRHSReceiver;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtWhile;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.factory.Factory;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.filter.PotentialVariableDeclarationFunction;
import spoon.support.StandardEnvironment;
import spoon.support.reflect.CtExtendedModifier;

public class ActionApplier {
	static Environment env = new StandardEnvironment();
	static spoon.reflect.visitor.PrettyPrinter pp = new spoon.reflect.visitor.DefaultJavaPrettyPrinter(env);

	public static <T extends Insert & AAction<Insert>> void applyAInsert(Factory factory, TreeContext ctx, T action) {
		ITree source = action.getSource();
		factory.createLocalVariableReference().getDeclaration();
		AbstractVersionedTree target = action.getTarget();
		AbstractVersionedTree parentTarget = target.getParent();
		// System.out.println("=======");
		// System.out.println(MyUtils.toPrettyString(ctx, source));
		// System.out.println(MyUtils.toPrettyString(ctx, target));
		// System.out.println(MyUtils.toPrettyString(ctx, parentTarget));
		String targetType = (String) target.getMetadata("type");
		switch (targetType) {
			case "LABEL": {
				// System.out.println("isLabel");
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent == null) {
					System.err.println(target);
					System.err.println(parentTarget);
				} else if (parent instanceof CtNamedElement) {
					((CtNamedElement) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtTypeReference) {
					CtTypeReference sps = (CtTypeReference) source.getParent()
							.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
					CtTypeReference ref = factory.Type().createReference(sps.getQualifiedName());
					if (parent.isParentInitialized()) {
						((CtTypeReference<?>) parent).replace(ref);
					}
					((CtTypeReference<?>) parent).setSimpleName(target.getLabel());
				} else if (parent instanceof CtBinaryOperator) {
					((CtBinaryOperator<?>) parent).setKind(MyUtils.getBinaryOperatorByName(target.getLabel()));
				} else if (parent instanceof CtUnaryOperator) {
					((CtUnaryOperator<?>) parent).setKind(MyUtils.getUnaryOperatorByName(target.getLabel()));
				} else if (parent instanceof CtLiteral) {
					if (target.getLabel().startsWith("\"")) {
						((CtLiteral<String>) parent).setValue(target.getLabel()
								.substring(1, target.getLabel().length() - 1).replace("\\\\", "\\").toString());
						((CtLiteral<String>) parent).setType(factory.Type().STRING);
						CtLiteral sl = (CtLiteral) source.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
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
						CtLiteral sl = (CtLiteral) source.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
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
						((CtLiteral<Long>) parent).setValue(
								Long.parseLong(target.getLabel().substring(0, target.getLabel().length() - 1)));
					} else if (target.getLabel().endsWith("D")) {
						((CtLiteral<Double>) parent).setValue(
								Double.parseDouble(target.getLabel().substring(0, target.getLabel().length())));
					} else {
						try {
							((CtLiteral<Object>) parent).setValue(Integer.parseInt(target.getLabel()));
						} catch (Exception e) {
							((CtLiteral<Double>) parent).setValue(Double.parseDouble(target.getLabel()));
						}
					}
				} else if (parent instanceof CtFieldAccess) {
					CtField var = factory.Query().createQuery(parent)
							.map(new PotentialVariableDeclarationFunction(target.getLabel())).first(CtField.class);
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
				} else if (parent instanceof CtVariableAccess) {
					CtVariableReference ref = factory.createLocalVariableReference();
					ref.setSimpleName(target.getLabel());
					((CtVariableAccess<?>) parent).setVariable(ref);
				} else if (parent instanceof CtConstructorCall) {
					CtConstructorCall sp = (CtConstructorCall) source.getParent()
							.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
					CtExecutableReference ref = factory.Executable()
							.createReference(sp.getExecutable().getType().getQualifiedName() + " "
									+ sp.getExecutable().getSignature().replace("(", "#<init>("));
					((CtConstructorCall<?>) parent).setExecutable(ref);
				} else if (parent instanceof CtInvocation) {
					CtExecutableReference ref = factory.createExecutableReference();
					ref.setSimpleName(target.getLabel());
					((CtInvocation<?>) parent).setExecutable(ref);
					ref.getDeclaration();
				} else if (parent instanceof CtTypeAccess) {
					CtTypeAccess sp = (CtTypeAccess) source.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
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
				} else if (parent instanceof CtThisAccess) { // TODO shouldn't get up to there
					// CtThisAccess ref = factory.createThisAccess();
					// ref.setSimpleName(target.getLabel());
					// ref.setImplicit(((CtTypeAccess<?>) parent).isImplicit());
					// ((CtThisAccess<?>) parent).setAccessedType(ref);
				} else if (parent instanceof CtOperatorAssignment) {
					((CtOperatorAssignment) parent).setKind(MyUtils.getBinaryOperatorByName(target.getLabel()));
				} else if (parent instanceof CtAssignment) { // TODO shouldn't get up to there
				} else if (parent instanceof CtReturn) { // TODO shouldn't get up to there
					// CtFieldWrite w = factory.createFieldWrite();
					// CtFieldReference v = factory.createFieldReference();
					// v.setSimpleName(target.getLabel());
					// w.setVariable(v);
					// ((CtAssignment) parent).setAssigned(w);

					// ((CtAssignment) parent).setLabel(target.getLabel());
				} else {
					System.err.println(parent.getClass());
				}
				break;
			}
			case "Interface": {
				// System.out.println("isInterface");
				CtInterface<?> interf = factory.createInterface();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				interf.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, interf);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent == null) {
					factory.getModel().getRootPackage().addType(interf);
				} else if (parent instanceof CtPackage) {
					interf.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
					((CtPackage) parent).addType(interf);
				} else {
					((CtType) parent).addNestedType(interf);
				}
				break;
			}
			case "Class": {
				// System.out.println("isClass");
				CtClass<?> clazz = factory.createClass();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				clazz.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, clazz);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent == null) {
					factory.getModel().getRootPackage().addType(clazz);
				} else if (parent instanceof CtPackage) {
					clazz.setSimpleName("PlaceHolder" + ((CtPackage) parent).getTypes().size());
					((CtPackage) parent).addType(clazz);
				} else if (parent instanceof CtType) {
					clazz.setSimpleName("PlaceHolder" + ((CtType) parent).getNestedTypes().size());
					((CtType) parent).addNestedType(clazz);
				} else {
					((CtNewClass<?>) parent).setAnonymousClass(clazz);
				}
				break;
			}
			case "RootPackage": {
				// System.out.println("isRootPackage");
				CtPackage pack = factory.getModel().getRootPackage();
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, pack);
				break;
			}
			case "Package": {
				// System.out.println("isPackage");
				CtPackage pack;
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent == null || parent instanceof CtRootPackage) {
					String name = "placeholderpack" + factory.getModel().getRootPackage().getPackages().size();
					pack = factory.Package().getOrCreate(name);
				} else {
					String name = "placeholderpack" + ((CtPackage) parent).getPackages().size();
					pack = factory.createPackage((CtPackage) parent, name);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, pack);
				break;
			}
			case "Method": {
				// System.out.println("isMethod");
				CtMethod<Object> method = factory.createMethod();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				method.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				method.setDefaultMethod(((CtMethod) sp).isDefaultMethod());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, method);
				CtType<?> parent = (CtType<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.addMethod(method);
				method.setSimpleName("placeHolder" + parent.getMethods().size());
				if (method.isAbstract()) {
					break;
				}
				if (parent instanceof CtInterface && !method.isDefaultMethod()) {
					break;
				}
				method.setBody(factory.createBlock());
				break;
			}
			case "RETURN_TYPE": {
				// System.out.println("isReturnType");
				CtTypeReference ref = factory.createTypeReference();
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				((CtTypedElement<?>) parent).setType(ref);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
				break;
			}
			case "MODIFIER": {
				// System.out.println("isMOdifier");
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtWrapper<CtExtendedModifier> mod = (CtWrapper<CtExtendedModifier>) source
						.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (!mod.getValue().isImplicit())
					((CtModifiable) parent).addModifier(mod.getValue().getKind());
				if (parent instanceof CtMethod && ((CtMethod) parent).isStatic()
						&& ((CtMethod) parent).getBody() == null)
					((CtMethod) parent).setBody(factory.createBlock());
				if (parent instanceof CtMethod && ((CtMethod) parent).isAbstract()
						&& ((CtMethod) parent).getBody() != null)
					((CtMethod) parent).setBody(null);
				break;
			}
			case "Field": {
				// System.out.println("isField");
				CtField<?> field = factory.createField();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				field.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, field);
				CtType<?> parent = (CtType<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				field.setSimpleName("placeHolder" + parent.getFields().size());
				parent.addField(field);
				break;
			}
			case "VARIABLE_TYPE": {
				// System.out.println("isVarType");
				CtTypeReference ref = factory.createTypeReference();
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				((CtTypedElement<?>) parent).setType(ref);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
				break;
			}
			case "Literal": {
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtLiteral created = factory.createLiteral();
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "BinaryOperator": {
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtBinaryOperator created = factory.createBinaryOperator();
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "UnaryOperator": {
				CtUnaryOperator created = factory.createUnaryOperator();
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				// if (parent == null) {
				// } else if (parent instanceof CtField) {
				// 	((CtField<?>) parent).setDefaultExpression(created);
				// } else if (parent instanceof CtReturn) {
				// 	((CtReturn<?>) parent).setReturnedExpression(created);
				// } else if (parent instanceof CtBodyHolder) {
				// 	addInBody(factory, target, created, (CtBodyHolder) parent);
				// } else if (parent instanceof CtIf) {
				// 	((CtIf) parent).setCondition(created);
				// } else {
				// 	throw new UnsupportedOperationException(
				// 			parent.getClass().toString() + " as a parent is no handled");
				// }
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "FieldRead": {
				// System.out.println("isFieldRead");
				CtFieldRead created = factory.createFieldRead();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "VariableRead": {
				// System.out.println("isVariableRead");
				CtVariableRead created = factory.createVariableRead();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "FieldWrite": {
				// System.out.println("isFieldWrite");
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				// CtVariable<?> var = factory.Query().createQuery(parent)
				//         .map(new PotentialVariableDeclarationFunction("simpleName")).first();
				CtFieldWrite created = factory.createFieldWrite();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setTarget(factory.createThisAccess(parent.getParent(CtType.class).getReference(), true));
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
				// System.out.println("isTypeAccess");
				CtTypeAccess created = factory.createTypeAccess();
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				created.setImplicit(((CtTypeAccess) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "AnonymousExecutable": {
				// System.out.println("isAnonymousExecutable");
				CtAnonymousExecutable created = factory.createAnonymousExecutable();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtClass<?> parent = (CtClass<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.addAnonymousExecutable(created);
				break;
			}
			case "Assignment": {
				// System.out.println("isAssignment");
				CtAssignment created = factory.createAssignment();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				// if (parent.getBody() == null) {
				//     parent.setBody(factory.createBlock());
				// }
				// int i = 0;
				// for (AbstractVersionedTree aaa : parentTarget.getAllChildren()) {
				//     if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) == null) {
				//         i++;
				//     }
				// }
				// parent.getBody().addStatement(i, created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "OperatorAssignment": {
				// System.out.println("isOperatorAssignment");
				CtOperatorAssignment created = factory.createOperatorAssignment();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				created.setKind(BinaryOperatorKind.MINUS);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				// if (parent.getBody() == null) {
				//     parent.setBody(factory.createBlock());
				// }
				// int i = 0;
				// for (AbstractVersionedTree aaa : parentTarget.getAllChildren()) {
				//     if (aaa.getMetadata("type").equals("MODIFIER")) {
				//         continue;
				//     }
				//     if (aaa.getMetadata("type").equals("RETURN_TYPE")) {
				//         continue;
				//     }
				//     if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null) {
				//         i++;
				//     }
				// }
				// parent.getBody().addStatement(i, created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Return": {
				// System.out.println("isReturn");
				CtReturn created = factory.createReturn();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Invocation": {
				// System.out.println("isInvocation");
				CtInvocation created = factory.createInvocation();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				// CtReturn<?> parent = (CtReturn<?>)
				// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setTarget(factory.createThisAccess(
						(parent instanceof CtType ? (CtType) parent : parent.getParent(CtType.class)).getReference(),
						true));
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ThisAccess": {
				// System.out.println("isThisAccess");
				CtThisAccess created = factory.createThisAccess();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				created.setImplicit(((CtThisAccess) sp).isImplicit());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtTargetedExpression<?, ?> parent = (CtTargetedExpression<?, ?>) parentTarget
						.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "Parameter": {
				// System.out.println("isParameter");
				CtParameter<?> created = factory.createParameter();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtExecutable<?> parent = (CtExecutable<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.addParameter(created);
				break;
			}
			case "TypeReference": {
				// System.out.println("isTypeReference");
				CtTypeReference created = factory.createTypeReference();
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (target.getLabel().equals(CtRole.CAST.name())) {
					((CtExpression) parent).addTypeCast(created);
				} else if (parent instanceof CtArrayTypeReference) {
					((CtArrayTypeReference) parent).setComponentType(created);
				} else {
					// ((CtExpression) parent).setComponentType(created);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "SUPER_CLASS": {
				// System.out.println("isTypeReference");
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtType<?> parentType = (CtType<?>) parentTarget.getParent()
						.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parentType.setSuperclass((CtTypeReference) parent);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, (CtTypeReference) parent);
				break;
			}
			// TODO add generators for following elements
			case "Constructor": {
				// System.out.println("isConstructor");
				CtConstructor cons = factory.createConstructor();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				cons.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, cons);
				CtClass<?> parent = (CtClass<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.addConstructor(cons);
				cons.setSimpleName("placeHolder" + parent.getMethods().size());
				cons.setBody(factory.createBlock());
				break;
			}
			case "INTERFACE": { // when inheriting interface
				// System.out.println("isINTERFACE");
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtType parentType = (CtType) parentTarget.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parentType.addSuperInterface((CtTypeReference) parent);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, (CtTypeReference) parent);
				break;
			}
			case "ConstructorCall": {
				// System.out.println("isConstructorCall");
				CtConstructorCall created = factory.createConstructorCall();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				// CtReturn<?> parent = (CtReturn<?>)
				// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else {
					addExpressionToParent(parent, created, target.getLabel());
				}
				// if (parent instanceof CtReturn) {
				// 	((CtReturn<?>) parent).setReturnedExpression(created);
				// } else if (parent instanceof CtExecutable) {
				// 	addInBody(factory, target, (CtStatement) created, (CtExecutable<?>) parent);
				// } else if (parent instanceof CtRHSReceiver) {
				// 	((CtRHSReceiver<?>) parent).setAssignment(created);
				// } else if (parent instanceof CtAbstractInvocation) {
				// 	((CtAbstractInvocation<?>) parent).addArgument(created);
				// } else {
				// 	throw new UnsupportedOperationException(
				// 			parent.getClass().toString() + " as a parent is no handled");
				// }
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Try": {
				// System.out.println("isTry");
				CtTry created = factory.createTry();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setBody(factory.createBlock());
				break;
			}
			case "Catch": {
				// System.out.println("isCatch");
				CtCatch created = factory.createCatch();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtTry parent = (CtTry) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.addCatcher(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				created.setBody(factory.createBlock());
				break;
			}
			case "If": {
				// System.out.println("isIf");
				CtIf created = factory.createIf();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Case": {
				// System.out.println("iscase");
				CtCase created = factory.createCase();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtSwitch parent = (CtSwitch) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.addCase(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "then": {
				// System.out.println("isthen");
				CtBlock created = factory.createBlock();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtIf parent = (CtIf) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.setThenStatement(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Block": {
				// System.out.println("isthen");
				CtBlock created = factory.createBlock();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				created.setImplicit(sp.isImplicit());
				switch (target.getLabel()) {
					case "THEN": {
						CtIf parent = (CtIf) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
						parent.setThenStatement(created);
						break;
					}
					case "ELSE": {
						CtIf parent = (CtIf) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
						parent.setElseStatement(created);
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
				// System.out.println("iselse");
				CtBlock created = factory.createBlock();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtIf parent = (CtIf) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.setElseStatement(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Conditional": {
				// System.out.println("isConditional");
				CtConditional created = factory.createConditional();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Switch": {
				// System.out.println("isSwitch");
				CtSwitch created = factory.createSwitch();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "While": {
				// System.out.println("isWhile");
				CtWhile created = factory.createWhile();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "For": {
				// System.out.println("isFor");
				CtFor created = factory.createFor();
				CtFor sp = (CtFor) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				if (((CtFor) sp).getBody() != null) {
					CtBlock<Object> block = factory.createBlock();
					block.setImplicit(sp.getBody().isImplicit());
					created.setBody(block);
				}
				break;
			}
			case "LocalVariable": {
				// System.out.println("isLocalVariable");
				CtLocalVariable<?> created = factory.createLocalVariable();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					created.setSimpleName("placeHolder" + (((CtBodyHolder) parent).getBody() == null ? 0
							: ((CtBlock) ((CtBodyHolder) parent).getBody()).getStatements().size()));
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					created.setSimpleName("placeHolder" + (((CtStatementList) parent) == null ? 0
							: ((CtStatementList) parent).getStatements().size()));
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "NewArray": {
				// System.out.println("isNewArray");
				CtNewArray created = factory.createNewArray();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				// CtReturn<?> parent = (CtReturn<?>)
				// parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "ArrayRead": {
				// System.out.println("isArrayRead");
				CtArrayRead created = factory.createArrayRead();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "ArrayWrite": {
				// System.out.println("isArrayRead");
				CtArrayWrite created = factory.createArrayWrite();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "VariableWrite": {
				// System.out.println("isVariableWrite");
				CtVariableWrite created = factory.createVariableWrite();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				break;
			}
			case "NewClass": {
				// System.out.println("isNewClass");
				CtNewClass created = factory.createNewClass();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				addExpressionToParent(parent, created, target.getLabel());
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "CatchVariable": {
				// System.out.println("isCatchVariable");
				CtCatchVariable created = factory.createCatchVariable();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				CtCatch parent = (CtCatch) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				parent.setParameter(created);
				break;
			}
			case "Break": {
				// System.out.println("isBreak");
				CtBreak created = factory.createBreak();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtStatementList)
					addInBody(factory, target, created, (CtStatementList) parent);
				else if (parent instanceof CtBodyHolder)
					addInBody(factory, target, created, (CtBodyHolder) parent);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Continue": {
				// System.out.println("isContinue");
				CtContinue created = factory.createContinue();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "ArrayTypeReference": {
				// System.out.println("isTypeReference");
				CtArrayTypeReference created = factory.createArrayTypeReference();
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtNewArray) {
					((CtNewArray) parent).setType(created);
				} else {
					((CtTypedElement) parent).setType(created);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				break;
			}
			case "Throw": {
				// System.out.println("isThrow");
				CtThrow created = factory.createThrow();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "Do": {
				// System.out.println("isDo");
				CtDo created = factory.createDo();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				if (parent instanceof CtBodyHolder) {
					addInBody(factory, target, created, (CtBodyHolder) parent);
				} else if (parent instanceof CtStatementList) {
					addInBody(factory, target, created, (CtStatementList) parent);
				}
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			case "THROWS": {
				// System.out.println("isTypeReference");
				CtTypeReference created = factory.createTypeReference();
				CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
				CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
				((CtMethod) parent).addThrownType(created);
				target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
				// created.setBody(factory.createBlock());
				break;
			}
			default: {
				System.err.println(targetType);
				throw new AssertionError(targetType + " is no handled");
				// throw new UnsupportedOperationException(targetType + " is no handled");
			}
		}
	}

	// private static void addExpressionToParent(CtElement parent, CtExpression created) {
	// 	addExpressionToParent(parent, created, null);
	// }

	private static void addExpressionToParent(CtElement parent, CtExpression created, String role) {
		if (parent instanceof CtField) {
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
		} else if (parent instanceof CtIf) {
			((CtIf) parent).setCondition(created);
		} else if (parent instanceof CtFor) {
			((CtFor) parent).setExpression(created);
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
		} else {
			throw new UnsupportedOperationException(parent.getClass().toString() + " as a parent is no handled");
		}
	}

	static void addInBody(Factory factory, AbstractVersionedTree target, CtStatement created, CtBodyHolder parent) {
		if (target.getLabel().equals(CtRole.FOR_INIT.name())) {
			((CtFor) parent).addForInit(created);
			return;
		} else if (target.getLabel().equals(CtRole.FOR_UPDATE.name())) {
			((CtFor) parent).addForUpdate(created);
			return;
		} else if (target.getLabel().equals(CtRole.EXPRESSION.name())) {
			if (parent instanceof CtFor) {
				((CtFor) parent).setExpression((CtExpression) created);
			} else if (parent instanceof CtDo) {
				((CtDo) parent).setLoopingExpression((CtExpression) created);
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
			if (aaa.getMetadata("type").equals("MODIFIER")) {
				continue;
			}
			if (aaa.getMetadata("type").equals("RETURN_TYPE")) {
				continue;
			}
			if (aaa.getMetadata("type").equals("THROWS")) {
				continue;
			}
			if (aaa.getMetadata("type").equals("LABEL")) {
				continue;
			}
			if (aaa.getMetadata("type").equals("Parameter")) {
				continue;
			}
			if (parent instanceof CtFor && aaa.getLabel().equals(CtRole.FOR_UPDATE.name())) {
				continue;
			}
			if (parent instanceof CtFor && aaa.getLabel().equals(CtRole.FOR_INIT.name())) {
				continue;
			}
			if (parent instanceof CtFor && aaa.getLabel().equals(CtRole.EXPRESSION.name())) {
				continue;
			}
			if (parent instanceof CtWhile && aaa.getLabel().equals(CtRole.EXPRESSION.name())) {
				continue;
			}
			if (parent instanceof CtDo && aaa.getLabel().equals(CtRole.EXPRESSION.name())) {
				continue;
			}
			if (parent instanceof CtCatch && aaa.getLabel().equals(CtRole.PARAMETER.name())) {
				continue;
			}
			if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null) {
				i++;
			}
			// if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) == null || aaa.getMetadata("type").equals("LABEL")
			//         || aaa.getMetadata("type").equals("MODIFIER") || aaa.getMetadata("type").equals("RETURN_TYPE")) {
			//     continue;
			// }
			// i++;
		}
		((CtBlock) parent.getBody()).addStatement(i, created);
	}

	static void addInBody(Factory factory, AbstractVersionedTree target, CtStatement created, CtStatementList parent) {
		int i = 0;
		for (AbstractVersionedTree aaa : target.getParent().getAllChildren()) {
			if (aaa.getMetadata("type").equals("MODIFIER")) {
				continue;
			}
			if (aaa.getMetadata("type").equals("RETURN_TYPE")) {
				continue;
			}
			if (aaa.getMetadata("type").equals("LABEL")) {
				continue;
			}
			if (aaa.getMetadata("type").equals("Parameter")) {
				continue;
			}
			if (aaa.getLabel().equals("EXPRESSION")) {
				continue;
			}
			if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) != null) {
				i++;
			}
			// if (aaa.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT) == null || aaa.getMetadata("type").equals("LABEL")
			//         || aaa.getMetadata("type").equals("MODIFIER") || aaa.getMetadata("type").equals("RETURN_TYPE")) {
			//     continue;
			// }
			// i++;
		}
		parent.addStatement(i, created);
	}

}