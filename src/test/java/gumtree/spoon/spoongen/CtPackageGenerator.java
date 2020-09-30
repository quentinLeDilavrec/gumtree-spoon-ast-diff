package gumtree.spoon.spoongen;

import java.util.Arrays;
import java.util.Collection;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.JavaIdentifiers;

public class CtPackageGenerator extends SpoonGenerator<CtPackage> {

    public CtPackageGenerator() {
        super(CtPackage.class);
    }

    @Override
    public CtPackage generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        CtPackage parent;
        Collection<CtPackage> all = factory.Package().getAll();
        parent = random.choose(all);
        generate(factory, random, status, parent instanceof CtRootPackage ? null : parent);
        return parent;
    }

    static int[][] shape = new int[][] { { 11, 100 }, { 21, 90, 5 }, { 41, 80, 10 }, { 61, 60, 20 }, { 80, 10, 45 },
            { 0 }, };

    public CtPackage generate(Factory factory, SourceOfRandomness random, GenerationStatus status, CtPackage parent) {
        String generatedName = generatePackageSimpleName(random);
        // random.choose(factory.Package().getAll())
        CtPackage pack = parent == null ? factory.Package().getOrCreate(generatedName)
                : factory.Package().create(parent, generatedName);
        int depth = pack.getQualifiedName().split("[.]").length - 1;
        int[] currShape = depth >= shape.length ? new int[] { 0 } : shape[depth];
        for (int i = 0; i < random.nextFloat(0, currShape[0]) / 10 - 0.1; i++) {
            if (random.nextInt(100) < currShape[1]) {
                CtPackageGenerator oneOf = gen().make(CtPackageGenerator.class);
                oneOf.generate(factory, random, status, pack);
            } else if (random.nextInt(100) < currShape[2]) {
                CtClassGenerator oneOf = gen().make(CtClassGenerator.class);
                CtType gene = oneOf.generate(factory, random, status, pack);
                if (random.nextInt(5) == 0) {
                    CtSuperClassedGenerator sndOf = gen().make(CtSuperClassedGenerator.class);
                    sndOf.generate(factory, random, status, gene);
                }
            } else {
                CtInterfaceGenerator oneOf = gen().make(CtInterfaceGenerator.class);
                CtType gene = oneOf.generate(factory, random, status, pack);
                if (random.nextInt(5) == 0) {
                    CtSuperClassedGenerator sndOf = gen().make(CtSuperClassedGenerator.class);
                    sndOf.generate(factory, random, status, gene);
                }
            }

        }
        return pack;
    }

    public static String generatePackageSimpleName(SourceOfRandomness random) {
        String generatedName;
        do {
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 0; i < random.nextInt(1, 5); i++) {
                switch (random.nextInt(i == 0 ? 1 : 6)) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        nameBuilder.append(random.nextChar('a', 'z'));
                        break;
                    case 4:
                        nameBuilder.append('_');
                        break;
                    case 5:
                        nameBuilder.append(random.nextChar('0', '9'));
                        break;
                    default:
                        break;
                }
            }
            generatedName = nameBuilder.toString();
        } while (!javax.lang.model.SourceVersion.isName(generatedName)
                && !JavaIdentifiers.isLegalJavaIdentifier(generatedName)
                && !Character.isUpperCase(generatedName.charAt(0)));
        return generatedName;
    }
}