package gumtree.spoon.apply;

import java.io.File;
import java.util.List;

import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;

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

public class MyUtils {

	public static Factory createFactory() {
		Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
		factory.getEnvironment().setNoClasspath(true);
		factory.getEnvironment().setCommentEnabled(false);
		return factory;
	}

	public static CtPackage makePkg(VirtualFile... resources) {
		Factory factory = MyUtils.createFactory();
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

	public static Factory makeFactory(VirtualFile... resources) {
		Factory factory = MyUtils.createFactory();
		factory.getModel().setBuildModelIsFinished(false);
		SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
		compiler.getFactory().getEnvironment().setLevel("OFF");
		compiler.getFactory().getEnvironment().setNoClasspath(true);
		for (VirtualFile resource : resources) {
			compiler.addInputSource(resource);
		}
		compiler.build();
		return factory;
	}

	public static String toTreeString(ITree tree) {
		StringBuilder b = new StringBuilder();
		aux(b, tree, 0);
		return b.toString();
	}

	private static void aux(StringBuilder b, ITree tree, int depth) {
		for (int i = 0; i < depth; i++) {
			b.append("\t");
		}
		if (tree instanceof AbstractVersionedTree) {
			AbstractVersionedTree tt = (AbstractVersionedTree) tree;
			
            if (tt.getInsertVersion() != null) {
                b.append(tt.getInsertVersion());
            }
            if (tt.getInsertVersion() != null || tt.getRemoveVersion() != null) {
                b.append("-");
            }
            if (tt.getRemoveVersion() != null) {
                b.append(tt.getRemoveVersion());
            }
            if (tt.getInsertVersion() != null || tt.getRemoveVersion() != null) {
                b.append(" ");
            }
		}
		if (tree.getMetadata("type") != null) {
			b.append(tree.getMetadata("type") + "@" + tree.getLabel());
		} else {
			b.append(tree.toShortString());
		}
		b.append("\n");
		if (tree instanceof AbstractVersionedTree) {
			for (ITree c : ((AbstractVersionedTree) tree).getAllChildren()) {
				aux(b, c, depth + 1);
			}
		} else {
			for (ITree c : tree.getChildren()) {
				aux(b, c, depth + 1);
			}
		}
	}

	public static Factory makeFactory(File... resources) {
		Factory factory = MyUtils.createFactory();
		factory.getModel().setBuildModelIsFinished(false);
		SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
		compiler.getFactory().getEnvironment().setLevel("OFF");
		for (File resource : resources) {
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
			b.append(MyUtils.indent(t) + t.toPrettyString(ctx) + "\n");
		return b.toString();
	}

	static String indent(ITree t) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < t.getDepth(); i++)
			b.append("\t");
		return b.toString();
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
				throw new UnsupportedOperationException(o);
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
				throw new UnsupportedOperationException(o);
		}
	}

	/**
	 * @return java source code representation of a pre or post unary operator.
	 */
	public static String getOperatorText(UnaryOperatorKind o) {
		switch (o) {
			case POS:
				return "+";
			case NEG:
				return "-";
			case NOT:
				return "!";
			case COMPL:
				return "~";
			case PREINC:
				return "++";
			case PREDEC:
				return "--";
			case POSTINC:
				return "++";
			case POSTDEC:
				return "--";
			default:
				throw new UnsupportedOperationException(o.name());
		}
	}

	/**
	 * @return java source code representation of a binary operator.
	 */
	public static String getOperatorText(BinaryOperatorKind o) {
		switch (o) {
			case OR:
				return "||";
			case AND:
				return "&&";
			case BITOR:
				return "|";
			case BITXOR:
				return "^";
			case BITAND:
				return "&";
			case EQ:
				return "==";
			case NE:
				return "!=";
			case LT:
				return "<";
			case GT:
				return ">";
			case LE:
				return "<=";
			case GE:
				return ">=";
			case SL:
				return "<<";
			case SR:
				return ">>";
			case USR:
				return ">>>";
			case PLUS:
				return "+";
			case MINUS:
				return "-";
			case MUL:
				return "*";
			case DIV:
				return "/";
			case MOD:
				return "%";
			case INSTANCEOF:
				return "instanceof";
			default:
				throw new UnsupportedOperationException(o.name());
		}
	}
}