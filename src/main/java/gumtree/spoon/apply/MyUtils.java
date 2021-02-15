package gumtree.spoon.apply;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionedTree;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import gumtree.spoon.apply.operations.MyCloneHelper;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.SpoonModelBuilder;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.DeclarationSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.reflect.path.CtPath;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.reflect.cu.position.SourcePositionImpl;

public class MyUtils {
    static Logger logger = Logger.getLogger(ApplierHelper.class.getName());

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

	/**
	 * also put a position with valid path start end
	 * @param <T>
	 * @param <U>
	 * @param tree
	 * @param version
	 * @return
	 */
	public static <T> ImmutablePair<CtElement, SourcePosition> toNormalizedPreciseSpoon(ITree tree, Version version) {
		CtElement ele = null;
		if (!(tree instanceof AbstractVersionedTree)){
			ele = (CtElement) tree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			if (ele == null) {
				return new ImmutablePair<>(null, null);
			}
		}
		if (ele == null) {
			Map<Version, CtElement> map = (Map<Version, CtElement>) tree
					.getMetadata(MyScriptGenerator.ORIGINAL_SPOON_OBJECT_PER_VERSION);
			if (map != null) {
				ele = map.get(version);
			}
		}
		if (ele == null) {
			if (((AbstractVersionedTree) tree).getInsertVersion() == version) {
				ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
			} else if (((AbstractVersionedTree) tree).getInsertVersion() == null) {
				if (((AbstractVersionedTree) tree).getRemoveVersion() == null) {
					ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
				} else if (version != null && version.partiallyCompareTo(
						((AbstractVersionedTree) tree).getRemoveVersion()) == Version.COMP_RES.INFERIOR) {
					ele = (CtElement) tree.getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
				} // if PARALLEL ?
			}
		}
		if (ele == null) {
			Map<Version, CtElement> map = (Map<Version, CtElement>) tree.getParent()
					.getMetadata(MyScriptGenerator.ORIGINAL_SPOON_OBJECT_PER_VERSION);
			if (map != null) {
				ele = map.get(version);
			}
		}
		if (ele == null) {
			ele = (CtElement) tree.getParent().getMetadata(VersionedTree.ORIGINAL_SPOON_OBJECT);
			if (ele == null) {
				ele = (CtElement) tree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			}
			if (ele == null) {
				ele = (CtElement) tree.getParent().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			}
		}
		if (ele == null) {
			return new ImmutablePair<>(null, null);
		}
		SourcePosition position = ele.getPosition();
		if (ele instanceof CtPackage) {
			return new ImmutablePair<>(ele, null);
		} else if (ele instanceof CtPackageReference && ele.isParentInitialized()
				&& ele.getParent() instanceof CtPackage) {
			return new ImmutablePair<>(ele, null);
		} else if ((position == null || !position.isValidPosition())) {
			// position = computePrecisePosition(ele);
			return new ImmutablePair<>(ele, computePrecisePosition(ele));
			// throw new RuntimeException(ele.getClass().toString());
		}
		return new ImmutablePair<CtElement, SourcePosition>(ele, position);
	}

	public static SourcePosition computePrecisePosition(CtElement ele) {
		CompilationUnit compilationUnit;
		int start, end;
		SourcePosition position = ele.getPosition();
		if (position != null && position.isValidPosition()
				&& (position instanceof SourcePositionImpl || ele instanceof CtTypeAccess)) {
			return position;
		} else if (ele instanceof CtReference || ele instanceof CtTypeAccess) {
			CtElement e = ele;
			position = ele.getPosition();
			int ss = 0, es = 0;
			while (position == null || !position.isValidPosition()) {
				CtRole role = e.getRoleInParent();
				if (e instanceof CtPackageReference && e.getParent() instanceof CtPackage) {
					return e.getPosition();
				}
				if (role == null) {
					if (!e.isParentInitialized()) {
						logger.warning(ele.getClass().toString() + " without a role nor a position nor a parent");
						break;
					} else if (e instanceof CtParameterReference && e.getParent() instanceof CtParameter) {
						CtElement old = e;
						e = e.getParent();
						position = e.getPosition();
						ss += e.toString().length() - ((CtParameterReference) old).getSimpleName().length();
						continue;
					} else {
						logger.warning(ele.getClass().toString() + " have no role and position");
						e = e.getParent();
						position = e.getPosition();
						continue;
					}
				}
				switch (role) {
					case ACCESSED_TYPE: {
						CtElement old = e;
						e = e.getParent();
						position = e.getPosition();
						try {
							ss += e.toString().length() - ((CtTypeReference) old).getSimpleName().length();
						} catch (Exception ee) {
						}
						break;
					}
					case PACKAGE_REF: {
						CtElement old = e;
						e = e.getParent().getParent();
						position = e.getPosition();
						try {
							es -= e.toString().length() - ((CtPackageReference) old).getSimpleName().length();
						} catch (Exception ee) {
						}
						break;
					}
					case TARGET: {
						CtElement old = e;
						e = e.getParent();
						position = e.getPosition();
						try {
							es = -(e.toString().length() - old.toString().length() - es);
						} catch (Exception ee) {
						}
						break;
					}
					case VARIABLE: {
						CtElement old = e;
						e = e.getParent().getParent();
						position = e.getPosition();
						try {
							es -= e.toString().length() - ((CtVariableReference) old).getSimpleName().length();
						} catch (Exception ee) {
						}
						break;
					}
					case TYPE: {
						CtElement old = e;
						e = e.getParent().getParent();
						position = e.getPosition();
						if (position instanceof DeclarationSourcePosition) {
							ss += ((DeclarationSourcePosition) position).getModifierSourceEnd()
									- ((DeclarationSourcePosition) position).getSourceStart();
							es -= ((DeclarationSourcePosition) position).getSourceEnd()
									- ((DeclarationSourcePosition) position).getNameStart();
						} else {
							position = e.getPosition();
						}
						break;// getSourceStart // getModifierSourceEnd // getNameStart //getSourceEnd
					}
					case ANNOTATION_TYPE: {
						CtElement old = e;
						e = e.getParent();
						position = e.getPosition();
						// System.out.println(CtRole.ANNOTATION_TYPE.toString() + " parent has position of type" + e.getPosition().getClass());
						break;
					}
					case DECLARING_TYPE: {
						CtElement old = e;
						e = e.getParent();
						position = e.getPosition();
						// System.out.println(CtRole.DECLARING_TYPE.toString() + " parent has position of type" + e.getPosition().getClass());
						// // TODO something like ? es = -(e.toString().length() - old.toString().length() - es);
						break;
					}
					default: {
						logger.warning(role.toString() + " not handled");
						e = e.getParent();
						position = e.getPosition();
						break;
					}
				}
			}
			compilationUnit = position.getCompilationUnit();
			// int correction = e.toString().length() - (position.getSourceEnd() - position.getSourceStart());
			start = position.getSourceStart() + ss;
			end = Math.max(start, position.getSourceEnd() + es);// + correction;
			position.getSourceStart();
		} else if (ele instanceof CtPackage) {
			return ele.getPosition();
		} else {
			position = ele.getPosition();
			if (position == null || !position.isValidPosition()) {
				logger.warning(ele.getClass().toString() + " should have a position, approximating to parent position");
				position = computePrecisePosition(ele.getParent()); // TODO fix the cause, might be in spoon
			}
			compilationUnit = position.getCompilationUnit();
			start = position.getSourceStart();
			end = position.getSourceEnd();
		}
		return new SourcePositionImpl(compilationUnit, start, end, compilationUnit.getLineSeparatorPositions());
	}
}