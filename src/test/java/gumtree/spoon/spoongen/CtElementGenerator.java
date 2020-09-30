package gumtree.spoon.spoongen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.ITree;
import com.pholser.junit.quickcheck.generator.Gen;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Generators;
import com.pholser.junit.quickcheck.internal.Ranges;
import com.pholser.junit.quickcheck.internal.generator.CompositeGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import gumtree.spoon.CloneVisitorNewFactory;
import gumtree.spoon.MyQcheckTryTest;
import spoon.Launcher;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.path.CtPath;
import spoon.support.visitor.equals.CloneHelper;

public class CtElementGenerator extends SpoonGenerator<CtElement> {
    // @Override
    // protected Generators gen() {
    // myGenerators.
    // return super.gen();
    // }

    public CtElementGenerator() {
        super(CtElement.class);
    }

    // @Override
    // protected Generators gen() {
    // Generators given = super.gen();
    // if (given instanceof CtElementGenerator) {
    // ((CtElementGenerator) given).setFactory(launcher.getFactory());
    // }
    // return given;
    // }

    // static List<Class<? extends CtElement>> possibilities =
    // Arrays.asList(CtLiteral.class);

    @Override
    public CtElement generate(Factory factory, SourceOfRandomness random, GenerationStatus status) {
        // int depth = this.range == null ? status.size() / 2 :
        // random.nextInt(this.range.min(), this.range.max());
        switch (random.nextInt(2)) {
            case 0: {
                SpoonGenerator<?> oneOf = (SpoonGenerator<?>) gen().type(random.choose(Arrays.asList(CtPackage.class//, CtType.class, CtClass.class, CtInterface.class
                )));
                CtElement res = oneOf.generate(factory, random, status);
                for (int i = 0; i < random.nextInt(5); i++) {
                    SpoonGenerator<?> oneOf2 = (SpoonGenerator<?>) gen()
                            .type(random.choose(Arrays.asList(CtPackage.class//, CtType.class, CtClass.class, CtInterface.class
                            )));
                    oneOf2.generate(factory, random, status);
                }
                return res;
            }
            case 1: {
                SpoonGenerator<?> oneOf = (SpoonGenerator<?>) gen().type(random.choose(Arrays.asList(CtPackage.class//, CtType.class, CtClass.class, CtInterface.class
                )));
                return oneOf.generate(factory, random, status);
            }
            default:
                throw new RuntimeException();
        }

        // switch (depth) {
        // case 0:
        // return
        // getFactory().createLiteral(gen().oneOf(Integer.class,String.class).generate(r,
        // s));
        // // return gen().oneOf(CtElement.class,CtElement.class).generate(r, s);
        // case 1:
        // return gen().type(CtElement.class).generate(r, s);
        // default:
        // s.setValue(TreeKeys.DEPTH, depth);
        // return gen().type(CtElement.class).generate(r, s);
        // }
    }

    static class MyClonerShrinker extends CloneHelper {
        private Launcher launcher;

        @Override
        public <T extends CtElement> T clone(T element) {
            final CloneVisitorNewFactory cloneVisitor = new CloneVisitorNewFactory(this, launcher.getFactory());
            cloneVisitor.scan(element);
            T clone = cloneVisitor.getClone();
            // if (element instanceof CtRootPackage) {
            //     clone = (T) ((CtPackage) clone).getParent();
            // }
            return clone;
        }

        public MyClonerShrinker(Factory pFactory) {
            this.launcher = new Launcher();
        }

        public Launcher getLauncher() {
            return launcher;
        }
    }

    @Override
    public List<CtElement> doShrink(SourceOfRandomness random, CtElement larger) {
        Factory factory = larger.getFactory();
        List<CtElement> res = new ArrayList<>();
        if (larger.getDirectChildren().size()<=0) {
            return res;
        }
        for (int i = 0; i < random.nextInt(10); i++) {
            CtElement clone = new MyClonerShrinker(factory).clone(larger);
            randomDel(random, random.choose(clone.getDirectChildren()));
            res.add(clone);
        }
        return res;
    }

    private void randomDel(SourceOfRandomness random, CtElement x) {
        switch (random.nextInt(2)) {
            case 0:
                List<CtElement> directChildren = x.getDirectChildren();
                if (directChildren.size()>0) {
                    randomDel(random, random.choose(directChildren));
                    break;
                }
            default:
                x.delete();
                break;
        }
    }

    // @Override
    // public boolean canRegisterAsType(Class<?> type) {
    // return false;
    // }

}

// @FunctionalInterface
// interface SpoonGen<T extends CtElement> extends Gen<T> {
// T generate(Factory factory, SourceOfRandomness random, GenerationStatus
// status);
// }

// public class SpoonGeneratorRepository implements GeneratorRepository {

// }