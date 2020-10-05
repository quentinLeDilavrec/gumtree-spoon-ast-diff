package gumtree.spoon.spoongen;

import java.util.Collection;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class CtSuperClassedGenerator extends SpoonGenerator<CtType> {

    public CtSuperClassedGenerator() {
        super(CtType.class);
    }

    @Override
    public CtType generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        CtType current = findOrGenerate(factory, random, status);
        return generate(factory, random, status, current);
    }

    private CtType findOrGenerate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        CtType current;
        Collection<CtType<?>> allTypes = factory.Type().getAll(true);
        switch (random.nextInt(3)) {
            case 0:
                if (allTypes.size() > 0) {
                    current = random.choose(allTypes);
                    break;
                }
            case 1:
                current = gen().make(CtClassGenerator.class).generate(factory, random, status);
                break;
            case 2:
                current = gen().make(CtInterfaceGenerator.class).generate(factory, random, status);
                break;
            default:
                throw null;
        }
        return current;
    }

    public CtType generate(Factory factory, SourceOfRandomness random, GenerationStatus status, CtType current) {
        // current.addSuperInterface(interfac)
        CtType found;
        do {
            found = findOrGenerate(factory, random, status);
        } while (!current.getReference().canAccess(found) && !found.getQualifiedName().equals(current.getQualifiedName()));
        String qualifiedName = found.getQualifiedName();
        CtTypeReference<Object> ref = factory.createReference(qualifiedName);
        current.setSuperclass(ref);
        return current;
    }
}