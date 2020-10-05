package gumtree.spoon.spoongen;

import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.pholser.junit.quickcheck.generator.ComponentizedGenerator;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import org.javaruntype.type.TypeParameter;
import org.javaruntype.type.WildcardTypeParameter;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.factory.Factory;
import spoon.support.reflect.code.CtLiteralImpl;

public class CtLiteralGenerator extends ComponentizedSpoonGenerator<CtLiteral> {

    public CtLiteralGenerator() {
        super(CtLiteral.class);
    }

    static Set<Class> accepted = new HashSet<>(Arrays.asList(new Class[] { Boolean.class, Byte.class, Character.class,
            Double.class, Float.class, Integer.class, String.class }));

    @Override
    public boolean canGenerateForParametersOfTypes(List<TypeParameter<?>> typeParameters) {
        if (!super.canGenerateForParametersOfTypes(typeParameters))
            return false;
        if (typeParameters.get(0) instanceof WildcardTypeParameter)
            return true;
        Class<?> rC = typeParameters.get(0).getType().getRawClass();
        if (accepted.contains(rC)) {
            return true;
        }
        return false;
    }

    @Override
    public CtLiteral generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        // Class contentType = r
        // .choose(Arrays.asList(String.class, Boolean.class, Integer.class,
        // Float.class, Double.class, null));
        // CtLiteral lit = new CtLiteralImpl<>();// getFactory().createLiteral(content);
        // lit.setValue(contentType == null ? null : gen().type(contentType).generate(r,
        // s));
        // // lit.setParent(getFactory().getModel().getRootPackage());
        Generator<?> generator = componentGenerators().get(0);
        Object value = generator.generate(random, status);
        return factory.createLiteral(value);
    }

    @Override
    public int numberOfNeededComponents() {
        return 1;
    }

}