package gumtree.spoon.spoongen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

public class CtFieldGenerator extends SpoonGenerator<CtField> {

    public CtFieldGenerator() {
        super(CtField.class);
    }

    @Override
    public CtField generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        return generate(factory, random, status, random.choose(factory.Type().getAll()));
    }

    static final Set<ModifierKind> vmods = Set.of(ModifierKind.PUBLIC, ModifierKind.PRIVATE, ModifierKind.PROTECTED);
    static final Set<ModifierKind> omods = Set.of(ModifierKind.FINAL, ModifierKind.STATIC, ModifierKind.TRANSIENT,
            ModifierKind.VOLATILE);

    public CtField generate(Factory factory, SourceOfRandomness random, GenerationStatus status, CtType parent) {
        Set<ModifierKind> mods = new HashSet<>();
        if (random.nextBoolean()) {
            mods.add(random.choose(vmods));
        }
        for (ModifierKind mod : omods) {
            if (random.nextBoolean()) {
                mods.add(mod);

            }
        }
        if (!parent.isStatic()) {
            mods.remove(ModifierKind.STATIC);
        }
        String generatedName = CtMethodGenerator.generateIdentifier(random);
        List<CtType<?>> allTypes = new ArrayList<>(factory.Type().getAll(true));
        allTypes = allTypes.stream().filter(x -> parent.getReference().canAccess(x)).collect(Collectors.toList());
        allTypes.add(factory.Type().BOOLEAN.getTypeDeclaration());
        allTypes.add(factory.Type().BYTE.getTypeDeclaration());
        allTypes.add(factory.Type().CHARACTER.getTypeDeclaration());
        // allTypes.add(factory.Type().COLLECTION.getTypeDeclaration());
        // // allTypes.add(factory.Type().DATE.getTypeDeclaration());
        allTypes.add(factory.Type().DOUBLE.getTypeDeclaration());
        allTypes.add(factory.Type().ENUM.getTypeDeclaration());
        allTypes.add(factory.Type().FLOAT.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        allTypes.add(factory.Type().INTEGER.getTypeDeclaration());
        // allTypes.add(factory.Type().ITERABLE.getTypeDeclaration());
        // allTypes.add(factory.Type().LIST.getTypeDeclaration());
        // allTypes.add(factory.Type().MAP.getTypeDeclaration());
        // allTypes.add(factory.Type().OBJECT.getTypeDeclaration());
        // allTypes.add(factory.Type().SET.getTypeDeclaration());
        // allTypes.add(factory.Type().SHORT.getTypeDeclaration());
        allTypes.add(factory.Type().STRING.getTypeDeclaration());
        CtType<?> typ = random.choose(allTypes);
        CtTypeReference<?> typR = factory.Type().createReference(typ);
        if (parent instanceof CtInterface) {
            mods.remove(ModifierKind.PRIVATE);
            mods.remove(ModifierKind.PROTECTED);
        }
        CtField field;
        int r = random.nextInt(100);
        if (r < 50) {
            try {
                Class paramClass = ((CtTypeReferenceImpl) typR).getActualClass();
                field = factory.createField(parent, mods, typR, generatedName,
                        ((SpoonGen<CtExpression>) gen().type(CtExpression.class, paramClass)).generate(factory, random,
                                status));
            } catch (Exception e) {
                field = factory.createField(parent, mods, typR, generatedName);
            }
        } else {
            field = factory.createField(parent, mods, typR, generatedName);
        }
        return field;
    }
}