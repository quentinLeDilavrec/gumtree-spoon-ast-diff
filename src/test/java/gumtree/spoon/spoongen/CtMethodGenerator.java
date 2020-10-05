package gumtree.spoon.spoongen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.reflect.visitor.JavaIdentifiers;

public class CtMethodGenerator extends SpoonGenerator<CtMethod> {

    public CtMethodGenerator() {
        super(CtMethod.class);
    }

    @Override
    public CtMethod generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        return generate(factory, random, status, random.choose(factory.Type().getAll()));
    }

    static final Set<ModifierKind> vmods = Set.of(ModifierKind.PUBLIC,ModifierKind.PRIVATE,ModifierKind.PROTECTED);
    static final Set<ModifierKind> omods = Set.of(ModifierKind.ABSTRACT,ModifierKind.FINAL,ModifierKind.STATIC,ModifierKind.TRANSIENT,ModifierKind.SYNCHRONIZED);

    public CtMethod generate(Factory factory, SourceOfRandomness random, GenerationStatus status, CtType parent) {
        Set<ModifierKind> mods = omods.stream().filter(x -> random.nextBoolean()).collect(Collectors.toSet());
        if (random.nextBoolean()) {
            mods.add(random.choose(vmods));
        }
        String generatedName = generateIdentifier(random);
        List<CtType<?>> allTypes = new ArrayList<>(factory.Type().getAll(true));
        allTypes = allTypes.stream().filter(x -> parent.getReference().canAccess(x)).collect(Collectors.toList());
        System.err.println(allTypes.stream().map(x -> x.getQualifiedName()).collect(Collectors.toList()));
        allTypes.add(factory.Type().BOOLEAN.getTypeDeclaration());
        allTypes.add(factory.Type().BOOLEAN_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().BYTE.getTypeDeclaration());
        allTypes.add(factory.Type().BYTE_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().CHARACTER.getTypeDeclaration());
        allTypes.add(factory.Type().CHARACTER_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().COLLECTION.getTypeDeclaration());
        allTypes.add(factory.Type().DATE.getTypeDeclaration());
        allTypes.add(factory.Type().DOUBLE.getTypeDeclaration());
        allTypes.add(factory.Type().DOUBLE_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().ENUM.getTypeDeclaration());
        allTypes.add(factory.Type().FLOAT.getTypeDeclaration());
        allTypes.add(factory.Type().FLOAT_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().ITERABLE.getTypeDeclaration());
        allTypes.add(factory.Type().LIST.getTypeDeclaration());
        allTypes.add(factory.Type().MAP.getTypeDeclaration());
        allTypes.add(factory.Type().OBJECT.getTypeDeclaration());
        allTypes.add(factory.Type().SET.getTypeDeclaration());
        allTypes.add(factory.Type().SHORT.getTypeDeclaration());
        allTypes.add(factory.Type().SHORT_PRIMITIVE.getTypeDeclaration());
        allTypes.add(factory.Type().STRING.getTypeDeclaration());
        allTypes.add(factory.Type().VOID.getTypeDeclaration());
        allTypes.add(factory.Type().VOID_PRIMITIVE.getTypeDeclaration());
        CtTypeReference<?> retT = factory.Type().createReference(random.choose(allTypes));
        System.err.println(retT.getQualifiedName());
        boolean isDefault = parent instanceof CtInterface ? random.nextInt(100) < 10 : false;
        if (isDefault) {
            mods.remove(ModifierKind.ABSTRACT);
            // mods.remove(ModifierKind.STATIC);
        }
        if (parent instanceof CtInterface) {
            mods.remove(ModifierKind.PRIVATE);
            mods.remove(ModifierKind.PROTECTED);
        }
        if (parent instanceof CtInterface && !isDefault) {
            mods.remove(ModifierKind.FINAL);
            mods.remove(ModifierKind.STRICTFP);
        }
        if (mods.contains(ModifierKind.ABSTRACT) && mods.contains(ModifierKind.FINAL)) {
            if (random.nextInt(100) < 50) {
                mods.remove(ModifierKind.ABSTRACT);
            } else {
                mods.remove(ModifierKind.FINAL);
            }
        }
        if (mods.contains(ModifierKind.ABSTRACT) && mods.contains(ModifierKind.STATIC)) {
            if (random.nextInt(100) < 50 && !(parent instanceof CtInterface)) {
                mods.remove(ModifierKind.ABSTRACT);
            } else {
                mods.remove(ModifierKind.STATIC);
            }
        }
        CtMethod meth;
        meth = factory.createMethod(parent, mods, retT, generatedName, Collections.emptyList(), Collections.emptySet());
        if (isDefault) {
            ((CtMethodImpl) meth).setDefaultMethod(true);
        }
        if (!meth.isAbstract()
                && (parent instanceof CtClass || meth.isStatic() || meth.isFinal() || meth.isDefaultMethod())) {
            CtBlock<Object> block = factory.createBlock();
            // if (random.nextBoolean())
            //     block.addStatement(gen().make(CtInvocationGenerator.class, gen().type(Integer.class)).generate(factory,
            //             random, status));
            if (!(retT.equals(factory.Type().VOID) || retT.equals(factory.Type().VOID_PRIMITIVE))
                    && !retT.isPrimitive()) {
                CtReturn<Object> retStatm = factory.createReturn();
                retStatm.setReturnedExpression(factory.createLiteral(null));
                block.addStatement(retStatm);
            } else if (!(retT.equals(factory.Type().VOID) || retT.equals(factory.Type().VOID_PRIMITIVE))
                    && retT.unbox().isPrimitive()) {
                if (random.nextBoolean()) {
                    CtReturn<Object> retStatm = factory.createReturn();
                    retStatm.setReturnedExpression(gen()
                            .make(CtExpressionGenerator.class,
                                    gen().type(((CtTypeReferenceImpl) retT.unbox()).getActualClass()))
                            .generate(factory, random, status));
                    block.addStatement(retStatm);
                } else {
                    CtReturn<Object> retStatm = factory.createReturn();
                    retStatm.setReturnedExpression(factory.createLiteral(null));
                    block.addStatement(retStatm);
                }
            }
            meth.setBody(block);
        }
        return meth;
    }

    public static String generateIdentifier(SourceOfRandomness random) {
        String generatedName;
        do {
            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(random.nextChar('a', 'z'));
            for (int i = 0; i < random.nextInt(0, 30); i++) {
                switch (random.nextInt(4)) {
                    case 0:
                        nameBuilder.append(random.nextChar('a', 'z'));
                        break;
                    case 1:
                        nameBuilder.append(random.nextChar('0', '9'));
                        break;
                    case 2:
                        nameBuilder.append(random.nextChar('A', 'Z'));
                        break;
                    case 3:
                        nameBuilder.append('_');
                        break;
                    default:
                        break;
                }
            }
            generatedName = nameBuilder.toString();// gen().type(String.class).generate(random, status);CtReferenceImpl
        } while (!javax.lang.model.SourceVersion.isName(generatedName)
                && !JavaIdentifiers.isLegalJavaIdentifier(generatedName)
                && !Character.isLowerCase(generatedName.charAt(0)));
        return generatedName;
    }
}