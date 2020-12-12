package gumtree.spoon.builder;

import java.util.Stack;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.declaration.ParentNotInitializedException;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;
import spoon.reflect.visitor.CtScanner;

public class TreeScanner extends CtScanner {
	public static final String NOTYPE = "<notype>";
	private final TreeContext treeContext;
	private final Stack<ITree> nodes = new Stack<>();
	boolean nodifiedLabel = true;

	TreeScanner(TreeContext treeContext, ITree root) {
		this.treeContext = treeContext;
		nodes.push(root);
	}

	@Override
	public void enter(CtElement element) {
		if (isToIgnore(element)) {
			super.enter(element);
			return;
		}

		String label = null;
		String nodeTypeName = getNodeType(element);

		if (nodifiedLabel) {
			label = nodeTypeName;
		} else {
			LabelFinder lf = new LabelFinder();
			lf.scan(element);
			label = lf.label;
		}
		pushNodeToTree(createNode(nodeTypeName, element, nodifiedLabel ? element.getRoleInParent().name() : label));

		int depthBefore = nodes.size();
		if (nodifiedLabel) {
			LabelFinder lf = new LabelFinder();
			lf.scan(element);
			if (lf.label != null && lf.label.length() > 0 && !(element instanceof CtSuperAccess) && !(element instanceof CtTypeAccess)
					&& (element instanceof CtNamedElement || element instanceof CtExpression
							|| (element instanceof CtReference && !(element instanceof CtWildcardReference)))) {
				this.addSiblingNode(lf.labEle==null?createNode("LABEL", lf.label):createNode("LABEL", lf.labEle, lf.label));
			}
		}
		new NodeCreator(this).scan(element);

		if (nodes.size() != depthBefore) {
			// contract: this should never happen
			throw new RuntimeException("too many nodes pushed");
		}
	}

	/**
	 * Ignore some element from the AST
	 * 
	 * @param element
	 * @return
	 */
	private boolean isToIgnore(CtElement element) {
		if (element instanceof CtStatementList && !(element instanceof CtCase)) {
			if (element.getRoleInParent() == CtRole.ELSE || element.getRoleInParent() == CtRole.THEN || element.getRoleInParent() == CtRole.STATEMENT) {
				return false;
			}
			return true;
		}

		if (element instanceof CtReference) {
			if (element.getRoleInParent() == CtRole.SUPER_TYPE || element.getRoleInParent() == CtRole.INTERFACE) {
				return false;
			} else if ((element.getRoleInParent() == CtRole.SUPER_TYPE
					|| element.getRoleInParent() == CtRole.INTERFACE)) {
				return false;
			} else if (element.getRoleInParent().equals(CtRole.CAST)) {
				return false;
			} else if (element.getParent() instanceof CtNewArray) {
				return false;
			} else if (((CtReference)element).getSimpleName().length()==0) {
				return true;
			} else if (element instanceof CtExecutableReference) {
				return true;
			// } else if (element instanceof CtTypeReference && (element.getParent() instanceof CtAbstractInvocation || element.getParent() instanceof CtExecutableReference)) {
			// 	return true;
			// } else if (element.getRoleInParent().equals(CtRole.DECLARING_TYPE) && (element.getParent() instanceof CtAbstractInvocation)) {
			// 	return true;
			} else if ((element.getParent() instanceof CtTypeAccess || element.getParent() instanceof CtTypeReference)) {
				// System.err.println(element.isImplicit());
				CtElement ele = element;
				boolean b = true;
				boolean t = false;
				while (ele != null) { // TODO look at parser 'cause an implicite this should not contain a non implicit target
					System.out.println(ele.getClass());
					System.out.println(ele.getRoleInParent());
					System.out.println(ele.isImplicit()); 
					if (ele instanceof CtExecutableReference)
						return true; 
					if (ele.getRoleInParent()!=null && ele.getRoleInParent().equals(CtRole.ANNOTATION_TYPE))
						return true; 
					if (ele.isImplicit())
						return true;
					if (ele instanceof CtParameterReference)
						return true;
					if (!ele.isParentInitialized())
						break;
					if (ele.getRoleInParent()!=null && ele.getRoleInParent().equals(CtRole.TYPE))
						t = true;
					if (ele.getRoleInParent()!=null && ele.getRoleInParent().equals(CtRole.DECLARING_TYPE))
						t = true;
					if (ele.getRoleInParent()!=null && ele.getRoleInParent().equals(CtRole.TARGET))
						b = false;
					if (ele instanceof CtAnnotation)
						return false;
					if (ele instanceof CtTargetedExpression)
						return b;
					if (ele instanceof CtTypeMember)
						return b && t;
					if (ele instanceof CtBlock)
						return true;
					if (ele instanceof CtNewArray)
						return true;
					ele = ele.getParent();
					// else if (ele instanceof CtExpression)
					// 	return b;
					// else if (ele instanceof CtStatement)
					// 	return b;
					// else if (ele instanceof CtType)
					// 	return b;
				}
				return true;
			} else if (element.getRoleInParent().equals(CtRole.TYPE)) {
				return true;
			// } else if (element.getRoleInParent().equals(CtRole.TYPE) && (element.getParent() instanceof CtExecutable || element.getParent() instanceof CtVariable || element.getParent() instanceof CtAbstractInvocation || element.getParent() instanceof CtVariable || element.getParent() instanceof CtAnnotation)) {
			// 	return true;
			} else {
				return true;
				// System.err.println(element.isImplicit());
				// return element.isImplicit();
			}
		}
		CtElement ele = element;
		// boolean b = element instanceof CtReference;
		while (ele != null) { // TODO look at parser 'cause an implicite this should not contain a non implicit target 
			if (ele.isImplicit())
				return true;
			if (!ele.isParentInitialized())
				break;
			ele = ele.getParent();
			if (ele instanceof CtBlock)
				break;
			if (ele instanceof CtNewArray)
				return false;
			// else if (element instanceof CtExpression)
			// 	return b;
			// else if (element instanceof CtStatement)
			// 	return b;
			// else if (element instanceof CtType)
			// 	return b;
		}
		return false;
	}

	@Override
	public void exit(CtElement element) {
		if (!isToIgnore(element)) {
			nodes.pop();
		}
		super.exit(element);
	}

	private void pushNodeToTree(ITree node) {
		ITree parent = nodes.peek();
		if (parent != null) { // happens when nodes.push(null)
			parent.addChild(node);
		}
		nodes.push(node);
	}

	void addSiblingNode(ITree node) {
		ITree parent = nodes.peek();
		if (parent != null) { // happens when nodes.push(null)
			parent.addChild(node);
		}
	}

	public String getNodeType(CtElement element) {
		String nodeTypeName = NOTYPE;
		if (element != null) {
			nodeTypeName = getTypeName(element.getClass().getSimpleName());
		}
		if (element instanceof CtInvocation && ((CtInvocation)element).getExecutable().isConstructor()) { // <init>
			// if (invocation.getExecutable().isConstructor()) { // <init>
			// It's a constructor (super or this)
			CtType<?> parentType;
			try {
				parentType = element.getParent(CtType.class);
			} catch (ParentNotInitializedException e) {
				parentType = null;
			}
			if (parentType == null || parentType.getQualifiedName() != null && parentType.getQualifiedName()
					.equals(((CtInvocation)element).getExecutable().getDeclaringType().getQualifiedName())) {
				nodeTypeName = "ThisInvocation";
			} else {
				nodeTypeName = "SuperInvocation";
			}
		}
		// if (element instanceof CtBlock) {
		// 	nodeTypeName = element.getRoleInParent().toString();
		// }
		return nodeTypeName;
	}

	private ITree createNode(String nodeTypeName, CtElement element, String label) {

		ITree newNode = createNode(nodeTypeName, label);
		newNode.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, element);
		element.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, newNode);
		return newNode;
	}

	String getTypeName(String simpleName) {
		// Removes the "Ct" at the beginning and the "Impl" at the end.
		if (simpleName.startsWith("Ct")) {
			simpleName = simpleName.substring(2);
		}
		if (simpleName.endsWith("Impl")) {
			simpleName = simpleName.substring(0, simpleName.length() - 4);
		}
		return simpleName;
	}

	public ITree createNode(String typeClass, String label) {
		ITree tree = treeContext.createTree(typeClass.hashCode(), label, typeClass);
		tree.setMetadata("type", treeContext.getTypeLabel(tree));
		return tree;
	}
}
