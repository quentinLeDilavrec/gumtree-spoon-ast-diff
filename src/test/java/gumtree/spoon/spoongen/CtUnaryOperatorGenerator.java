package gumtree.spoon.spoongen;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.pholser.junit.quickcheck.generator.ComponentizedGenerator;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.java.lang.*;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.factory.Factory;
import spoon.support.reflect.code.CtLiteralImpl;
import spoon.support.reflect.code.CtUnaryOperatorImpl;

public class CtUnaryOperatorGenerator extends ComponentizedSpoonGenerator<CtUnaryOperator> {
    List<Generator> forNegGen = new ArrayList<>();
    List<Generator> forComplGen = new ArrayList<>();

    public CtUnaryOperatorGenerator() {
        super(CtUnaryOperator.class);
        IntegerGenerator integerGenerator = new IntegerGenerator();
        integerGenerator.configure(forNeg);
        forNegGen.add(integerGenerator);
        LongGenerator longGenerator = new LongGenerator();
        longGenerator.configure(forNeg);
        forNegGen.add(longGenerator);
        FloatGenerator floatGenerator = new FloatGenerator();
        floatGenerator.configure(forNeg);
        forNegGen.add(floatGenerator);
        DoubleGenerator doubleGenerator = new DoubleGenerator();
        doubleGenerator.configure(forNeg);
        forNegGen.add(doubleGenerator);
    }

    @Override
    public CtUnaryOperator<?> generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {

        CtUnaryOperator created = factory.createUnaryOperator();// getFactory().createLiteral(content);
        Generator<?> operandGenerator = componentGenerators().get(0);
        if (operandGenerator.canRegisterAsType(Number.class)) {
            operandGenerator.configure(buildInRange());
            created.setKind(UnaryOperatorKind.NEG);
            Generator<CtLiteral> gene = gen().make((Class) CtLiteralGenerator.class, operandGenerator);
            created.setOperand(gene.generate(random, status));
            return created;
        } else {
            System.err.println(operandGenerator.canRegisterAsType(Number.class));
            System.err.println(operandGenerator.getClass());
            UnaryOperatorKind opKind = random.choose(Arrays.asList(UnaryOperatorKind.values()));
            created.setKind(UnaryOperatorKind.COMPL);
            // TODO use return type and make it the other way choosing the op given a return
            // type/operandGen
            switch (UnaryOperatorKind.COMPL) {
                case COMPL:
                case POS:
                case NEG:
                    Generator<CtLiteral> gene = gen().make((Class) CtLiteralGenerator.class, random.choose(forNegGen));
                    created.setOperand(gene.generate(random, status));
                    break;
                default:
                    created.setKind(UnaryOperatorKind.NEG);
                    Generator<CtLiteral> gene2 = gen().make((Class) CtLiteralGenerator.class, random.choose(forNegGen));
                    created.setOperand(gene2.generate(random, status));
                    break;
            }
            return created;
        }

    }

    @Override
    public int numberOfNeededComponents() {
        return 1;
    }

    private AnnotatedElement buildInRange() {

        return new AnnotatedElement() {

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> arg0) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[]{forNeg};
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return null;
            }

        };
    }

    private InRange forNeg = new InRange() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return (Class) InRange.class;
        }

        @Override
        public byte minByte() {
            return Byte.MIN_VALUE;
        }

        @Override
        public byte maxByte() {
            return Byte.MAX_VALUE;
        }

        @Override
        public short minShort() {
            return Short.MIN_VALUE;
        }

        @Override
        public short maxShort() {
            return Short.MAX_VALUE;
        }

        @Override
        public char minChar() {
            return Character.MIN_VALUE;
        }

        @Override
        public char maxChar() {
            return Character.MAX_VALUE;
        }

        @Override
        public int minInt() {
            return Integer.MIN_VALUE;
        }

        @Override
        public int maxInt() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long minLong() {
            return Long.MIN_VALUE;
        }

        @Override
        public long maxLong() {
            return Long.MAX_VALUE;
        }

        @Override
        public float minFloat() {
            return Float.MIN_VALUE;
        }

        @Override
        public float maxFloat() {
            return Float.MAX_VALUE;
        }

        @Override
        public double minDouble() {
            return Double.MIN_VALUE;
        }

        @Override
        public double maxDouble() {
            return Double.MAX_VALUE;
        }

        @Override
        public String min() {
            return "0";
        }

        @Override
        public String max() {
            return "";
        }

        @Override
        public String format() {
            return "";
        }

    };
}