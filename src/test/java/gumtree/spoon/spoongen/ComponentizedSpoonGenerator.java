package gumtree.spoon.spoongen;

import java.util.List;

import com.pholser.junit.quickcheck.generator.ComponentizedGenerator;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.Launcher;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;

public abstract class ComponentizedSpoonGenerator<T extends CtElement> extends ComponentizedGenerator<T>
        implements SpoonGen<T> {

    public ComponentizedSpoonGenerator(Class<T> type) {
        super(type);
    }

    @Override
    public final T generate(SourceOfRandomness random, GenerationStatus status) {
        Launcher launcher = new Launcher();
        Factory factory = launcher.getFactory();
        return generate(factory, random, status);
    }

    public abstract T generate(Factory factory, SourceOfRandomness random, GenerationStatus status);

    @Override
    public List<T> doShrink(SourceOfRandomness random, T larger) {
        return super.doShrink(random, larger);
    }
}