package gumtree.spoon.builder;

import java.util.Stack;

import com.github.gumtreediff.gen.Registry.Factory;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import gumtree.spoon.apply.MyUtils;
import javassist.CtField;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtParameter;
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
import spoon.reflect.reference.CtVariableReference;
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
		element.setPosition(MyUtils.computePrecisePosition(element));
		pushNodeToTree(createNode(nodeTypeName, element, nodifiedLabel ? element.getRoleInParent().name() : label));

		int depthBefore = nodes.size();
		if (nodifiedLabel) {
			LabelFinder lf = new LabelFinder();
			lf.scan(element);
			if (lf.label != null && lf.label.length() > 0 && !(element instanceof CtSuperAccess)
					&& !(element instanceof CtTypeAccess) && !(element instanceof CtAbstractInvocation)
					&& !(element instanceof CtAnnotation) && !(element instanceof CtVariableAccess)
					&& (element instanceof CtNamedElement || element instanceof CtExpression
							|| (element instanceof CtReference && !(element instanceof CtWildcardReference)))) {
				this.addSiblingNode(
						lf.labEle == null ? createNode("LABEL", lf.label) : createNode("LABEL", lf.labEle, lf.label));
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
			if (element.getRoleInParent() == CtRole.ELSE || element.getRoleInParent() == CtRole.THEN
					|| element.getRoleInParent() == CtRole.STATEMENT) {
				return false;
			}
			return true;
		}
		return isToIgnoreAux(element);
	}

	private boolean isToIgnoreAux(CtElement element) {//element.getPosition().getCompilationUnit().getImports().get(0).getReference().
		if (element.isImplicit()
				&& !(element.getRoleInParent() == CtRole.ELSE || element.getRoleInParent() == CtRole.THEN)) {//((CtForEach)element).getVariable()
			if (element instanceof CtPackageReference && element.isParentInitialized()) {
				CtElement parent = element.getParent();
				if (!parent.isParentInitialized()) {
					return true;
				}
				if (parent.getRoleInParent() == null) {
					return true;
				} else if (parent instanceof CtTypeReference) {
					switch (parent.getRoleInParent()) {
						case DECLARING_TYPE:
							return true;
						case CAST:
						case ACCESSED_TYPE:
						case SUPER_TYPE:
						case TYPE:
						default: {
							if (((CtTypeReference) parent).getDeclaration() == null) {
							} else {
								String tq = ((CtTypeReference) parent).getDeclaration().getTopLevelType()
										.getQualifiedName();
								CtType pt = parent instanceof CtType ? (CtType) parent : parent.getParent(CtType.class);
								if (pt == null) {
									System.err.println("missing parent type of" + parent.getClass());
									System.err.println(parent);
									return true;
								}
								if (pt.getTopLevelType() == null) {
									System.err.println("missing top level of" + parent.getClass());
									System.err.println(parent);
									return true;
								}
								String top = pt.getTopLevelType().getQualifiedName();
								return tq.equals(top) ? true : isToIgnoreAux(element.getParent());
							}
						}
						// return true;
					}
				} else {
					return true;
				}
			} else {
				return true;
			}
		} else if (!element.isParentInitialized()) {
			return false;
		}
		if (element.getRoleInParent() != null) {
			if (element.getRoleInParent().equals(CtRole.TYPE) && element.getParent() instanceof CtReference
					&& (!element.getParent().isParentInitialized()
							|| (!(element.getParent().getParent() instanceof CtNewArray)
									&& !(element.getParent().getParent() instanceof CtVariable)
									&& !(element.getParent().getParent() instanceof CtConstructorCall)))) {
				return true;
			}
			if (element.getRoleInParent().equals(CtRole.EXECUTABLE_REF) && element.getParent() instanceof CtType) {
				return true;
			}
			if (element.getRoleInParent().equals(CtRole.TYPE) && element.getParent() instanceof CtExpression) {
				return true;
			}
			// if (element.getRoleInParent().equals(CtRole.TARGET) 
			//   && element.getParent() instanceof CtThisAccess
			//   && element.getParent().isImplicit()
			//   && element.getParent().isParentInitialized() 
			//   && element.getParent().getParent() instanceof CtFieldWrite) {
			// 	return isToIgnoreAux(element.getParent().getParent());
			// }
			// if (element.getRoleInParent().equals(CtRole.TARGET) 
			//   && (element.getParent() instanceof CtThisAccess || element.getParent() instanceof CtSuperAccess)
			//   && element.getParent().isImplicit()) {
			// 	return isToIgnoreAux(element.getParent().getParent());
			// }
			if (element.getRoleInParent().equals(CtRole.TARGET) && element.getParent() instanceof CtThisAccess) {
				return true;
			}
			if (element.getRoleInParent().equals(CtRole.TARGET) && element.getParent() instanceof CtSuperAccess) {
				return true;
			}
			if (element.getRoleInParent().equals(CtRole.DECLARING_TYPE)
					&& element.getParent() instanceof CtExecutableReference) {
				return true;
			}
			if (element.getRoleInParent().equals(CtRole.DECLARING_TYPE)
					&& element.getParent() instanceof CtVariableReference) {
				return true;
			}
			if (element.getRoleInParent().equals(CtRole.ARGUMENT_TYPE)) {
				return true;
			}
			// if (element.toString().length()==0) {
			if (element instanceof CtReference && ((CtReference)element).getSimpleName().length()==0) {
				return true;
			}
		}
		return isToIgnoreAux(element.getParent());
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
		if (element instanceof CtInvocation && ((CtInvocation) element).getExecutable().isConstructor()) { // <init>
			// if (invocation.getExecutable().isConstructor()) { // <init>
			// It's a constructor (super or this)
			CtType<?> parentType;
			try {
				parentType = element.getParent(CtType.class);
			} catch (ParentNotInitializedException e) {
				parentType = null;
			}
			if (parentType == null || parentType.getQualifiedName() != null && parentType.getQualifiedName()
					.equals(((CtInvocation) element).getExecutable().getDeclaringType().getQualifiedName())) {
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
