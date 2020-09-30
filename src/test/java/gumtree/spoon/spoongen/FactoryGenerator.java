package gumtree.spoon.spoongen;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Generators;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.Launcher;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;

public class FactoryGenerator extends Generator<Factory> {
    public FactoryGenerator() {
        super(Factory.class);
    }

    Generators ctGenerator;
    private Launcher launcher;

    // @Override
    // protected Generators gen() {
    //     Generators given = super.gen();
    //     if (given instanceof CtElementGenerator) {
    //         ((CtElementGenerator)given).setFactory(launcher.getFactory());
    //     }
    //     return given;
    // }

    @Override
    public Factory generate(SourceOfRandomness r, GenerationStatus s) {
        launcher = new Launcher();
        return launcher.getFactory();
    }
}