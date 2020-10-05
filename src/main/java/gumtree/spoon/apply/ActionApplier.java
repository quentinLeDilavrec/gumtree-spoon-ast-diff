package gumtree.spoon.apply;

import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtRHSReceiver;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
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
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.filter.PotentialVariableDeclarationFunction;
import spoon.support.reflect.CtExtendedModifier;

public class ActionApplier {

	public static <T extends Insert & AAction<Insert>> void applyAInsert(Factory factory, TreeContext ctx, T action) {
	    ITree source = action.getSource();
	    factory.createLocalVariableReference().getDeclaration();
	    AbstractVersionedTree target = action.getTarget();
	    AbstractVersionedTree parentTarget = target.getParent();
	    System.out.println("=======");
	    System.out.println(MyUtils.toPrettyString(ctx, source));
	    System.out.println(MyUtils.toPrettyString(ctx, target));
	    System.out.println(MyUtils.toPrettyString(ctx, parentTarget));
	    String targetType = (String) target.getMetadata("type");
	    switch (targetType) {
	        case "LABEL": {
	            System.out.println("isLabel");
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            if (parent == null) {
	                System.err.println(target);
	                System.err.println(parentTarget);
	            } else if (parent instanceof CtNamedElement) {
	                ((CtNamedElement) parent).setSimpleName(target.getLabel());
	            } else if (parent instanceof CtTypeReference) {
	                ((CtTypeReference<?>) parent).setSimpleName(target.getLabel());
	            } else if (parent instanceof CtBinaryOperator) {
	                ((CtBinaryOperator<?>) parent).setKind(MyUtils.getBinaryOperatorByName(target.getLabel()));
	            } else if (parent instanceof CtUnaryOperator) {
	                ((CtUnaryOperator<?>) parent).setKind(MyUtils.getUnaryOperatorByName(target.getLabel()));
	            } else if (parent instanceof CtLiteral) {
	                if (target.getLabel().startsWith("\"")) {
	                    ((CtLiteral<String>) parent).setValue(
	                            target.getLabel().substring(1, target.getLabel().length() - 1).replace("\\\\", "\\"));
	                } else if (target.getLabel().startsWith("'")) {
	                    ((CtLiteral<Character>) parent)
	                            .setValue(target.getLabel().substring(1, target.getLabel().length() - 1).charAt(0));
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
	                        .map(new PotentialVariableDeclarationFunction(target.getLabel())).first();
	                // CtVariableReference ref = factory.createFieldReference();
	                // ref.setSimpleName(target.getLabel());
	                if (var == null) {
	                    CtFieldAccess sps = (CtFieldAccess)source.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	                    CtFieldReference field = factory.Field().createReference(sps.getVariable().getType().getQualifiedName()+" "+sps.getVariable().getQualifiedName());
	                    ((CtFieldAccess<?>) parent).setVariable(field);
	                    ((CtFieldAccess<?>) parent).setTarget(factory.createTypeAccess(field.getDeclaringType(),false));
	                } else {
	                    ((CtFieldAccess<?>) parent).setVariable(var.getReference());
	                    ((CtFieldAccess<?>) parent).setTarget(factory.createTypeAccess(var.getReference().getDeclaringType(),false));
	                }
	            } else if (parent instanceof CtVariableAccess) {
	                CtVariableReference ref = factory.createLocalVariableReference();
	                ref.setSimpleName(target.getLabel());
	                ((CtVariableAccess<?>) parent).setVariable(ref);
	            } else if (parent instanceof CtInvocation) {
	                CtExecutableReference ref = factory.createExecutableReference();
	                ref.setSimpleName(target.getLabel());
	                ((CtInvocation<?>) parent).setExecutable(ref);
	                ref.getDeclaration();
	                ((CtInvocation<?>) parent).getTarget();
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
	                CtTargetedExpression parentparent = (CtTargetedExpression) parent.getParent();
	                parentparent.setTarget((CtTypeAccess<?>) parent);
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
	            System.out.println("isInterface");
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
	            System.out.println("isClass");
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
	            } else {
	                clazz.setSimpleName("PlaceHolder" + ((CtType) parent).getNestedTypes().size());
	                ((CtType) parent).addNestedType(clazz);
	            }
	            break;
	        }
	        case "RootPackage": {
	            System.out.println("isRootPackage");
	            CtPackage pack = factory.getModel().getRootPackage();
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, pack);
	            break;
	        }
	        case "Package": {
	            System.out.println("isPackage");
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
	            System.out.println("isMethod");
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
	            System.out.println("isReturnType");
	            CtTypeReference ref = factory.createTypeReference();
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            ((CtTypedElement<?>) parent).setType(ref);
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
	            break;
	        }
	        case "MODIFIER": {
	            System.out.println("isMOdifier");
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
	            System.out.println("isField");
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
	            System.out.println("isVarType");
	            CtTypeReference ref = factory.createTypeReference();
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            ((CtTypedElement<?>) parent).setType(ref);
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
	            break;
	        }
	        case "Literal": {
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            CtExpression created = factory.createLiteral();
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            if (parent == null) {
	            } else if (parent instanceof CtRHSReceiver) {
	                ((CtRHSReceiver<?>) parent).setAssignment(created);
	            } else if (parent instanceof CtReturn) {
	                ((CtReturn<?>) parent).setReturnedExpression(created);
	            } else if (parent instanceof CtUnaryOperator) {
	                ((CtUnaryOperator<?>) parent).setOperand(created);
	            } else {
	                CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	                if (sp.getRoleInParent().equals(CtRole.LEFT_OPERAND))
	                    ((CtBinaryOperator<?>) parent).setLeftHandOperand(created);
	                else if (sp.getRoleInParent().equals(CtRole.RIGHT_OPERAND))
	                    ((CtBinaryOperator<?>) parent).setRightHandOperand(created);
	                else
	                    throw new UnsupportedOperationException(sp.getRoleInParent().name() + " role not supported");
	            }
	            break;
	        }
	        case "BinaryOperator": {
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            CtExpression created = factory.createBinaryOperator();
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            if (parent instanceof CtField) {
	                ((CtField<?>) parent).setDefaultExpression(created);
	            } else if (parent instanceof CtReturn) {
	                ((CtReturn<?>) parent).setReturnedExpression(created);
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
	            break;
	        }
	        case "UnaryOperator": {
	            CtExpression created = factory.createUnaryOperator();
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            if (parent == null) {
	            } else if (parent instanceof CtField) {
	                ((CtField<?>) parent).setDefaultExpression(created);
	            } else if (parent instanceof CtReturn) {
	                ((CtReturn<?>) parent).setReturnedExpression(created);
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
	            break;
	        }
	        case "FieldRead": {
	            System.out.println("isFieldRead");
	            CtFieldRead created = factory.createFieldRead();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	
	            if (parent instanceof CtInvocation) {
	                ((CtInvocation<?>) parent).setTarget(created);
	            } else if (parent instanceof CtUnaryOperator) {
	                ((CtUnaryOperator<?>) parent).setOperand(created);
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
	            break;
	        }
	        case "VariableRead": {
	            System.out.println("isVariableRead");
	            CtVariableRead created = factory.createVariableRead();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            if (parent instanceof CtInvocation) {
	                ((CtInvocation<?>) parent).addArgument(created);
	            } else if (parent instanceof CtBinaryOperator) {
	                if (sp.getRoleInParent().equals(CtRole.LEFT_OPERAND)) {
	                    ((CtBinaryOperator<?>) parent).setLeftHandOperand(created);
	                } else if (sp.getRoleInParent().equals(CtRole.RIGHT_OPERAND)) {
	                    ((CtBinaryOperator<?>) parent).setRightHandOperand(created);
	                } else {
	                    throw new UnsupportedOperationException(sp.getRoleInParent().name() + " role not supported");
	                }
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
	            break;
	        }
	        case "FieldWrite": {
	            System.out.println("isFieldWrite");
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            // CtVariable<?> var = factory.Query().createQuery(parent)
	            //         .map(new PotentialVariableDeclarationFunction("simpleName")).first();
	            CtFieldWrite created = factory.createFieldWrite();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            if (parent instanceof CtUnaryOperator) {
	                ((CtUnaryOperator<?>) parent).setOperand(created);
	            } else if (parent instanceof CtAssignment) {
	                ((CtAssignment<?, ?>) parent).setAssigned(created);
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
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
	            System.out.println("isTypeAccess");
	            CtTypeAccess created = factory.createTypeAccess();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            created.setImplicit(((CtTypeAccess) sp).isImplicit());
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            CtTargetedExpression<?, ?> parent = (CtTargetedExpression<?, ?>) parentTarget
	                    .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            if (parent instanceof CtFieldAccess) {
	                ((CtFieldAccess<?>) parent).setTarget(created);
	            } else if (parent instanceof CtThisAccess) {
	                ((CtThisAccess<?>) parent).setTarget(created);
	            } else if (parent instanceof CtInvocation) {
	                ((CtInvocation<?>) parent).setTarget(created);
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
	            break;
	        }
	        case "AnonymousExecutable": {
	            System.out.println("isAnonymousExecutable");
	            CtAnonymousExecutable created = factory.createAnonymousExecutable();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            CtClass<?> parent = (CtClass<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            parent.addAnonymousExecutable(created);
	            break;
	        }
	        case "Assignment": {
	            System.out.println("isAssignment");
	            CtAssignment created = factory.createAssignment();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            CtAnonymousExecutable parent = (CtAnonymousExecutable) parentTarget
	                    .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            addInBody(factory, target, created, parent);
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
	            System.out.println("isOperatorAssignment");
	            CtOperatorAssignment created = factory.createOperatorAssignment();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            created.setKind(BinaryOperatorKind.MINUS);
	            CtAnonymousExecutable parent = (CtAnonymousExecutable) parentTarget
	                    .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	
	            addInBody(factory, target, created, parent);
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
	            System.out.println("isReturn");
	            CtReturn created = factory.createReturn();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            CtMethod parent = (CtMethod) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            addInBody(factory, target, (CtStatement) created, parent);
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            break;
	        }
	        case "Invocation": {
	            System.out.println("isInvocation");
	            CtInvocation created = factory.createInvocation();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            // CtReturn<?> parent = (CtReturn<?>)
	            // parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setTarget(factory.createThisAccess(
	                    (parent instanceof CtType ? (CtType) parent : parent.getParent(CtType.class)).getReference(),
	                    true));
	            if (parent instanceof CtReturn) {
	                ((CtReturn<?>) parent).setReturnedExpression(created);
	            } else if (parent instanceof CtExecutable) {
	                addInBody(factory, target, (CtStatement) created, (CtExecutable<?>) parent);
	            } else if (parent instanceof CtRHSReceiver) {
	                ((CtRHSReceiver<?>) parent).setAssignment(created);
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            break;
	        }
	        case "ThisAccess": {
	            System.out.println("isThisAccess");
	            CtThisAccess created = factory.createThisAccess();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            created.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            created.setImplicit(((CtThisAccess) sp).isImplicit());
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, created);
	            CtTargetedExpression<?, ?> parent = (CtTargetedExpression<?, ?>) parentTarget
	                    .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            if (parent instanceof CtFieldAccess) {
	                ((CtFieldAccess<?>) parent).setTarget(created);
	            } else if (parent instanceof CtInvocation) {
	                ((CtInvocation<?>) parent).setTarget(created);
	            } else {
	                throw new UnsupportedOperationException(
	                        parent.getClass().toString() + " as a parent is no handled");
	            }
	            break;
	        }
	        case "Parameter": {
	            System.out.println("isParameter");
	            CtParameter<?> field = factory.createParameter();
	            CtElement sp = (CtElement) source.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            field.setPosition(sp.getPosition()); // TODO how do we handle Compilation unit and position?
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, field);
	            CtMethod<?> parent = (CtMethod<?>) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            parent.addParameter(field);
	            break;
	        }
	        case "TypeReference": {
	            System.out.println("isTypeReference");
	            CtTypeReference ref = factory.createTypeReference();
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            // ((CtType<?>) parent).setSuperclass(ref);
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ref);
	            break;
	        }
	        case "SUPER_CLASS": {
	            System.out.println("isTypeReference");
	            CtElement parent = (CtElement) parentTarget.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            CtType<?> parentType = (CtType<?>) parentTarget.getParent()
	                    .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	            parentType.setSuperclass((CtTypeReference) parent);
	            target.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, (CtTypeReference) parent);
	            break;
	        }
	        default: {
	            System.err.println(targetType);
	            throw new AssertionError(targetType + " is no handled");
	            // throw new UnsupportedOperationException(targetType + " is no handled");
	        }
	    }
	}

	static void addInBody(Factory factory, AbstractVersionedTree target, CtStatement created,
	        CtExecutable<?> parent) {
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
	        if (aaa.getMetadata("type").equals("LABEL")) {
	            continue;
	        }
	        if (aaa.getMetadata("type").equals("Parameter")) {
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
	    parent.getBody().addStatement(i, created);
	}
    
}