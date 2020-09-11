package gumtree.spoon;

import java.util.List;

import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;

import gumtree.spoon.apply.AAction;
import gumtree.spoon.apply.operations.MyCloneHelper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.SpoonModelBuilder;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

public class Utils {

	public static Factory createFactory() {
		Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
		factory.getEnvironment().setNoClasspath(true);
		factory.getEnvironment().setCommentEnabled(false);
		return factory;
	}

	static CtPackage makePkg(VirtualFile... resources) {
		Factory factory = Utils.createFactory();
		factory.getModel().setBuildModelIsFinished(false);
		SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
		compiler.getFactory().getEnvironment().setLevel("OFF");
		for (VirtualFile resource : resources) {
			compiler.addInputSource(resource);
		}
		compiler.build();
		CtPackage rp = factory.getModel().getRootPackage();
		return rp;
	}

	static Factory makeFactory(VirtualFile... resources) {
		Factory factory = Utils.createFactory();
		factory.getModel().setBuildModelIsFinished(false);
		SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
		compiler.getFactory().getEnvironment().setLevel("OFF");
		for (VirtualFile resource : resources) {
			compiler.addInputSource(resource);
		}
		compiler.build();
		return factory;
	}

	public static String toPrettyString(TreeContext ctx, ITree tree) {
		if (tree == null)
			return "null";
		return tree.toPrettyString(ctx);
	}

	public static String toPrettyTree(TreeContext ctx, ITree tree) {
		if (tree == null)
			return "null";
		StringBuilder b = new StringBuilder();
		for (ITree t : TreeUtils.preOrder(tree))
			b.append(Utils.indent(t) + t.toPrettyString(ctx) + "\n");
		return b.toString();
	}

	static String indent(ITree t) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < t.getDepth(); i++)
			b.append("\t");
		return b.toString();
	}

	static void applyAAction(AAction action) {
		MyCloneHelper cloneHelper = new MyCloneHelper();
		ITree leftNode = (ITree) action.getSource();
		AbstractVersionedTree rightNode = (AbstractVersionedTree) action.getTarget();
		CtElement leftSpoon = (CtElement) leftNode.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		assert leftSpoon != null;
		if (action instanceof Insert) {
			System.out.println(leftSpoon);
			CtElement rightSpoon = (CtElement) rightNode.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			System.out.println(rightSpoon);
			CtElement rightParentSpoon = (CtElement) rightNode.getParent()
					.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			System.out.println(rightParentSpoon);
			if (rightNode.getMetadata("type").equals("LABEL")) {
				System.out.println("ignore label insert"); // DO not add it in the first place ?
			} else {
				System.out.println("do the insert");
				List<AbstractVersionedTree> childrenAtInsTime = rightNode.getChildren(rightNode.getAddedVersion());
				if (childrenAtInsTime.size() > 0 && childrenAtInsTime.get(0).getMetadata("type").equals("LABEL")) {
					System.out.println("has a label");
					System.out.println(childrenAtInsTime.size());
					System.out.println(leftSpoon.getClass());
					System.out.println(leftSpoon.clone());
					CtElement clone = cloneHelper.clone(leftSpoon);
					rightNode.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, clone);
					if (clone instanceof CtTypeParameter) {
						((CtType) rightParentSpoon).addFormalCtTypeParameter((CtTypeParameter) clone);
						// clone.setParent(rightParentSpoon);                        
					}
					System.out.println(childrenAtInsTime.get(0).toShortString());
				}
				// for (AbstractVersionedTree firstC : rightNode.getChildren(rightNode.getAddedVersion())) {
				//     System.out.println(firstC.toShortString());
				// }
			}

		} else if (action instanceof Update) {
			assert leftNode.hasSameType(rightNode);
			if (leftNode.getMetadata("type").equals("LABEL")) {
				CtElement rightParentSpoon = (CtElement) rightNode.getParent()
						.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

			}
		} else if (action instanceof Delete) {
			System.out.println(leftSpoon);

		}
		System.out.println(action);
	}

	public static UnaryOperatorKind getUnaryOperatorByName(String o) {
		switch (o) {
			case "POS":
				return UnaryOperatorKind.POS;
			case "NEG":
				return UnaryOperatorKind.NEG;
			case "NOT":
				return UnaryOperatorKind.NOT;
			case "COMPL":
				return UnaryOperatorKind.COMPL;
			case "PREINC":
				return UnaryOperatorKind.PREINC;
			case "POSTINC":
				return UnaryOperatorKind.POSTINC;
			case "PREDEC":
				return UnaryOperatorKind.PREDEC;
			case "POSTDEC":
				return UnaryOperatorKind.POSTDEC;
			default:
				throw new RuntimeException("Unsupported operator " + o);
		}
	}

	public static BinaryOperatorKind getBinaryOperatorByName(String o) {
		switch (o) {
			case "OR":
				return BinaryOperatorKind.OR;
			case "AND":
				return BinaryOperatorKind.AND;
			case "BITOR":
				return BinaryOperatorKind.BITOR;
			case "BITXOR":
				return BinaryOperatorKind.BITXOR;
			case "BITAND":
				return BinaryOperatorKind.BITAND;
			case "EQ":
				return BinaryOperatorKind.EQ;
			case "NE":
				return BinaryOperatorKind.NE;
			case "LT":
				return BinaryOperatorKind.LT;
			case "GT":
				return BinaryOperatorKind.GT;
			case "LE":
				return BinaryOperatorKind.LE;
			case "GE":
				return BinaryOperatorKind.GE;
			case "SL":
				return BinaryOperatorKind.SL;
			case "SR":
				return BinaryOperatorKind.SR;
			case "USR":
				return BinaryOperatorKind.USR;
			case "PLUS":
				return BinaryOperatorKind.PLUS;
			case "MINUS":
				return BinaryOperatorKind.MINUS;
			case "MUL":
				return BinaryOperatorKind.MUL;
			case "DIV":
				return BinaryOperatorKind.DIV;
			case "MOD":
				return BinaryOperatorKind.MOD;
			case "INSTANCEOF":
				return BinaryOperatorKind.INSTANCEOF;
			default:
				throw new RuntimeException("Unsupported operator " + o);
		}
	}

	public static UnaryOperatorKind getUnaryOperator(String o) {
		switch (o) {
			case "+":
				return UnaryOperatorKind.POS;
			case "-":
				return UnaryOperatorKind.NEG;
			case "!":
				return UnaryOperatorKind.NOT;
			case "~":
				return UnaryOperatorKind.COMPL;
			// case "++":
			// 	return UnaryOperatorKind.PREINC;
			case "++":
				return UnaryOperatorKind.POSTINC;
			// case "--":
			// 	return UnaryOperatorKind.PREDEC;
			case "--":
				return UnaryOperatorKind.POSTDEC;
			default:
				throw new RuntimeException("Unsupported operator " + o);
		}
	}

	public static BinaryOperatorKind getBinaryOperator(String o) {
		switch (o) {
			case "||":
				return BinaryOperatorKind.OR;
			case "&&":
				return BinaryOperatorKind.AND;
			case "|":
				return BinaryOperatorKind.BITOR;
			case "^":
				return BinaryOperatorKind.BITXOR;
			case "&":
				return BinaryOperatorKind.BITAND;
			case "==":
				return BinaryOperatorKind.EQ;
			case "!=":
				return BinaryOperatorKind.NE;
			case "<":
				return BinaryOperatorKind.LT;
			case ">":
				return BinaryOperatorKind.GT;
			case "<=":
				return BinaryOperatorKind.LE;
			case ">=":
				return BinaryOperatorKind.GE;
			case "<<":
				return BinaryOperatorKind.SL;
			case ">>":
				return BinaryOperatorKind.SR;
			case ">>>":
				return BinaryOperatorKind.USR;
			case "+":
				return BinaryOperatorKind.PLUS;
			case "-":
				return BinaryOperatorKind.MINUS;
			case "*":
				return BinaryOperatorKind.MUL;
			case "/":
				return BinaryOperatorKind.DIV;
			case "%":
				return BinaryOperatorKind.MOD;
			case "instanceof":
				return BinaryOperatorKind.INSTANCEOF;
			default:
				throw new RuntimeException("Unsupported operator " + o);
		}
	}
}