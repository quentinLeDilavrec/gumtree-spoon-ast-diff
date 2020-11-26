package gumtree.spoon;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.tree.ITree;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.java.lang.AbstractStringGenerator;
import com.pholser.junit.quickcheck.generator.GeneratorConfiguration;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.junit.Before;
import org.junit.runner.RunWith;

import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
import gumtree.spoon.spoongen.CtElementGenerator;
import gumtree.spoon.spoongen.CtExpressionGenerator;
import gumtree.spoon.spoongen.CtLiteralGenerator;
import gumtree.spoon.spoongen.CtUnaryOperatorGenerator;
import gumtree.spoon.spoongen.FactoryGenerator;
import spoon.ContractVerifier;
import spoon.compiler.Environment;
import spoon.reflect.CtModelImpl.CtRootPackage;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.support.StandardEnvironment;
import spoon.support.reflect.code.CtLiteralImpl;

@RunWith(JUnitQuickcheck.class)
public class MyQcheckTryTest { // StringProperties
    @Before
    public void initProps() {
        System.setProperty("nolabel", "true");
    }

    // @Target({ PARAMETER, FIELD, ANNOTATION_TYPE, TYPE_USE })
    // @Retention(RUNTIME)
    // @GeneratorConfiguration
    // static public @interface Depth {
    // int min()

    // default 0;

    // int max();
    // }

    // static final class TreeKeys {
    // static final GenerationStatus.Key<Integer> DEPTH = new
    // GenerationStatus.Key<>("depth", Integer.class);

    // private TreeKeys() {
    // throw new UnsupportedOperationException();
    // }
    // }

    // @Property
    // public void something(@From(CtElementGenerator.class) CtElement ast) {
    // spoon.reflect.visitor.PrettyPrinter pp = new
    // spoon.reflect.visitor.DefaultJavaPrettyPrinter(
    // new StandardEnvironment());
    // assertEquals(pp.prettyprint(ast), pp.prettyprint(ast));
    // }

    // @Property
    // public void something2(@From(CtElementGenerator.class) CtElement ast,
    // @From(CtElementGenerator.class) CtElement ast2) {
    // assertEquals(ast, ast2);
    // }

    @Property
    public void something3(@From(CtElementGenerator.class) @When(seed = 601495490727140223L) CtElement right) {
        // ContractVerifier cv = new
        // spoon.ContractVerifier(right.getFactory().getModel().getRootPackage());
        // try {
        // cv.verify();
        // } catch (AssertionError e) {
        // assumeNoException(e);
        // }
        ApplyTestHelper.onInsert(right);
    }
}