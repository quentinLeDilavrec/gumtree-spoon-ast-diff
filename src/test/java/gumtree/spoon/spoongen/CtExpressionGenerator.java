package gumtree.spoon.spoongen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import org.javaruntype.type.TypeParameter;

import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.factory.Factory;
import spoon.support.reflect.code.CtLiteralImpl;
import spoon.support.reflect.code.CtUnaryOperatorImpl;

public class CtExpressionGenerator extends ComponentizedSpoonGenerator<CtExpression> {
    public CtExpressionGenerator() {
        super(CtExpression.class);
    }

    static List<Class> nestingPossibilities = Arrays
            .asList(new Class[] { CtLiteral.class, CtInvocation.class, CtUnaryOperator.class });

    // static Set<Class> acceptedParam = new TreeSet<>(Arrays.asList(new Class[] {
    // Boolean.class, Byte.class, Character.class,
    // Double.class, Float.class, Integer.class, String.class }));

    // @Override
    // public boolean canGenerateForParametersOfTypes(List<TypeParameter<?>>
    // typeParameters) {
    // if (!super.canGenerateForParametersOfTypes(typeParameters))
    // return false;
    // Class<?> rC = typeParameters.get(0).getType().getRawClass();
    // if (acceptedParam.contains(rC)) {
    // return true;
    // }
    // return false;
    // }

    @Override
    public CtExpression generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        // Class contentType = r
        // .choose(Arrays.asList(String.class, Boolean.class, Integer.class,
        // Float.class, Double.class, null));
        // CtUnaryOperator created = new CtUnaryOperatorImpl<>();//
        // getFactory().createLiteral(content);
        // created.setOperand(contentType == null ? null : );
        // // lit.setParent(getFactory().getModel().getRootPackage());
        List<Class> params = componentGenerators().stream().map(x -> x.types().stream().filter(y->!y.isPrimitive()).findFirst().get())
                .collect(Collectors.toList());
        while (true) {
            Generator<CtExpression> one = gen().type(random.choose(nestingPossibilities), params.toArray(new Class[0]));
            one.addComponentGenerators(componentGenerators());
            CtExpression generated = ((SpoonGen<CtExpression>)one).generate(factory,random, status);
            if (generated != null) {
                return generated;
            }
        }
    }

    @Override
    public int numberOfNeededComponents() {
        return 1;
    }
}