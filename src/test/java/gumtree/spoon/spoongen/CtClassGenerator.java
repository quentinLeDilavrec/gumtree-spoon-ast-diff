package gumtree.spoon.spoongen;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.JavaIdentifiers;
import spoon.support.reflect.reference.CtReferenceImpl;

public class CtClassGenerator extends SpoonGenerator<CtClass> {

    public CtClassGenerator() {
        super(CtClass.class);
    }

    @Override
    public CtClass generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        CtElement parent;
        switch (random.nextInt(3)) {
            case 0:
                Collection<CtType<?>> allTypes = factory.Type().getAll(true);
                if (allTypes.size() > 0) {
                    parent = random.choose(allTypes);
                    break;
                }
            case 1:
                Collection<CtPackage> all = factory.Package().getAll();
                if (all.size() > 0) {
                    parent = random.choose(all);
                    break;
                }
            default:
                parent = factory.Package().getRootPackage();
        }
        return generate(factory, random, status, parent);
    }

    public ModifierKind oneOf(SourceOfRandomness random, ModifierKind... mods) {
        return random.choose(mods);
    }

    public CtClass generate(Factory factory, SourceOfRandomness random, GenerationStatus status, CtElement parent) {
        String generatedName = generateTypeSimpleName(random);
        CtClass clazz;
        Set<ModifierKind> mods = new HashSet<>();
        if (parent instanceof CtPackage) {
            clazz = factory.createClass((CtPackage) parent, generatedName);
            if (random.nextBoolean()) {
                mods.add(ModifierKind.PUBLIC);
            }
        } else if (parent instanceof CtType) {
            // TODO create Class should allow CtType or at least CtInterface
            clazz = factory.createClass();
            clazz.setSimpleName(generatedName);
            ((CtType) parent).addNestedType(clazz);
            CtType parentparent = parent.getParent(CtType.class);
            if ((parentparent == null) || (((CtType) parent).isStatic() && random.nextBoolean())) {
                mods.add(ModifierKind.STATIC);
            }
            if (random.nextBoolean()) {
                mods.add(oneOf(random, ModifierKind.ABSTRACT, ModifierKind.FINAL));
            }
            if (random.nextBoolean()) {
                mods.add(oneOf(random, ModifierKind.PUBLIC, ModifierKind.PRIVATE));
            }
        } else {
            throw new RuntimeException();
        }
        clazz.setModifiers(mods);
        for (int i = 0; i < random.nextInt(5); i++) {
            CtType tmp = null;
            switch (random.nextInt(9)) {
                case 0: {
                    CtClassGenerator oneOf = gen().make(CtClassGenerator.class);
                    tmp = oneOf.generate(factory, random, status, clazz);
                    break;
                }
                case 1: {
                    CtInterfaceGenerator oneOf = gen().make(CtInterfaceGenerator.class);
                    tmp = oneOf.generate(factory, random, status, clazz);
                    break;
                }
                case 2:
                case 3:
                case 4: {
                    CtMethodGenerator oneOf = gen().make(CtMethodGenerator.class);
                    oneOf.generate(factory, random, status, clazz);
                    break;
                }
                case 5:
                case 6:
                case 7:
                case 8: {
                    CtFieldGenerator oneOf = gen().make(CtFieldGenerator.class);
                    oneOf.generate(factory, random, status, clazz);
                    break;
                }
            }
            switch (random.nextInt(3)) {
                case 0: {
                    if (tmp != null) {
                        CtSuperClassedGenerator oneOf = gen().make(CtSuperClassedGenerator.class);
                        oneOf.generate(factory, random, status, tmp);
                        break;
                    }
                }
            }
        }
        return clazz;
    }

    public static String generateTypeSimpleName(SourceOfRandomness random) {
        String generatedName;
        do {
            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(random.nextChar('A', 'Z'));
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
                    case 4:
                        nameBuilder.append('$');
                        break;
                    default:
                        break;
                }
                // JavaIdentifiers
            }
            generatedName = nameBuilder.toString();// gen().type(String.class).generate(random, status);CtReferenceImpl
        } while (!javax.lang.model.SourceVersion.isName(generatedName)
                && !JavaIdentifiers.isLegalJavaIdentifier(generatedName)
                && !Character.isUpperCase(generatedName.charAt(0)));
        return generatedName;
    }

}