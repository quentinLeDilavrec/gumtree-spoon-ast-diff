package gumtree.spoon.spoongen;

import java.util.List;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;

public interface SpoonGen<T extends CtElement> {
    /**
     * 
     * @param factory
     * @param random
     * @param status
     * @return
     */
    public T generate(Factory factory, SourceOfRandomness random, GenerationStatus status);

}