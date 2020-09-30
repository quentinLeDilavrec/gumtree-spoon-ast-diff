package gumtree.spoon.spoongen;

import java.util.Collections;
import java.util.List;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.Launcher;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;

public abstract class SpoonGenerator<T extends CtElement> extends Generator<T> implements SpoonGen<T> {

    public SpoonGenerator(List<Class<T>> types) {
        super(types);
    }

    public SpoonGenerator(Class<T> type) {
        super(type);
    }

    // protected Factory getFactory() {
    // if (factory != null) {
    // return factory;
    // }
    // Launcher launcher = new Launcher();
    // return launcher.getFactory();
    // }
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

    // public void setFactory(Factory factory) {
    // this.factory = factory;
    // }
}