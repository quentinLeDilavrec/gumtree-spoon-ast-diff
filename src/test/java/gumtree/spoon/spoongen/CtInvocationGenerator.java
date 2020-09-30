package gumtree.spoon.spoongen;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;

public class CtInvocationGenerator extends ComponentizedSpoonGenerator<CtInvocation> {
    public CtInvocationGenerator() {
        super(CtInvocation.class);
    }

    @Override
    public CtInvocation generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        List<CtType<?>> all = factory.Type().getAll(true);
        if (all.size()<=0) {
            return null;
        }
        CtType ccc = random.choose(all);
        Set<CtMethod> bbb = ccc.getMethods();
        CtMethod aaa = bbb.size() > 0 ? random.choose(bbb)
                : gen().make(CtMethodGenerator.class).generate(factory, random, status, ccc);

        CtExpression target = factory.createTypeAccess(ccc.getReference(),false); // TODO try with implicit and parent scope
        // gen().type(CtExpression.class, null).generate(random, status);

        CtExecutableReference ere = aaa.getReference();
        // gen().type(CtExecutableReference.class).generate(random, status)
        CtInvocation call = factory.createInvocation(target, ere, Collections.emptyList());
        return call;
    }

    @Override
    public int numberOfNeededComponents() {
        return 1;
    }
}