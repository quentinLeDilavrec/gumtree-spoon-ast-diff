package gumtree.spoon.builder;

import java.util.Stack;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

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
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ParentNotInitializedException;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtReference;
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
			if (lf.label != null && lf.label.length() > 0 && !(element instanceof CtSuperAccess)
					&& (element instanceof CtNamedElement || element instanceof CtExpression
							|| (element instanceof CtReference && !(element instanceof CtWildcardReference)))) {
				this.addSiblingNode(createNode("LABEL", lf.label));
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
			} else {
				return true;
			}
		}

		// boolean b = element instanceof CtReference;
		while (element != null) { // TODO look at parser 'cause an implicite this should not contain a non implicit target 
			if (element.isImplicit())
				return true;
			if (!element.isParentInitialized())
				break;
			element = element.getParent();
			if (element instanceof CtBlock)
				break;
			if (element instanceof CtNewArray)
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
