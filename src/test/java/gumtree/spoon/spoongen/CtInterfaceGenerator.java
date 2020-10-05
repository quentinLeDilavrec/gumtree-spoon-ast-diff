package gumtree.spoon.spoongen;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.java.lang.StringGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.JavaIdentifiers;

public class CtInterfaceGenerator extends SpoonGenerator<CtInterface> {

    public CtInterfaceGenerator() {
        super(CtInterface.class);
    }

    @Override
    public CtInterface generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
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
    
    static final Set<ModifierKind> vmods = Set.of(ModifierKind.PUBLIC, ModifierKind.PRIVATE);

    public CtInterface generate(Factory factory, SourceOfRandomness random, GenerationStatus status, CtElement parent) {
        String generatedName = CtClassGenerator.generateTypeSimpleName(random);
        CtInterface interf;
        if (parent instanceof CtPackage) {
            interf = factory.createInterface((CtPackage) parent, generatedName);
        } else if (parent instanceof CtType) {
            interf = factory.createInterface((CtType) parent, generatedName);
        } else {
            throw new RuntimeException();
        }
        interf.setModifiers(Set.of(random.choose(vmods)));
        for (int i = 0; i < random.nextInt(5); i++) {
            CtType tmp = null;
            switch (random.nextInt(6)) {
                case 0: {
                    CtClassGenerator oneOf = gen().make(CtClassGenerator.class);
                    tmp = oneOf.generate(factory, random, status, interf);
                    break;
                }
                case 1: {
                    CtInterfaceGenerator oneOf = gen().make(CtInterfaceGenerator.class);
                    tmp = oneOf.generate(factory, random, status, interf);
                    break;
                }
                case 2:
                case 3: {
                    CtMethodGenerator oneOf = gen().make(CtMethodGenerator.class);
                    oneOf.generate(factory, random, status, interf);
                    break;
                }
                case 4:
                case 5: {
                    CtFieldGenerator oneOf = gen().make(CtFieldGenerator.class);
                    oneOf.generate(factory, random, status, interf);
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
        return interf;
    }
}