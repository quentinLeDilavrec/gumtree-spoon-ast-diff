package gumtree.spoon.diff.operations;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.ITree;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import gumtree.spoon.builder.Json4SpoonGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

public abstract class Operation<T extends Action> {
	private final T action;

	public Operation(T action) {
		this.action = action;
	}

	/** use {@link #getSrcNode()} or {@link #getDstNode()} instead. */
	@Deprecated
	public CtElement getNode() {
		return (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	}

	public T getAction() {
		return action;
	}

	public abstract OperationKind getKind();

	@Override
	public String toString() {
		return toStringAction(action);
	}

	private String toStringAction(Action action) {
		String newline = System.getProperty("line.separator");
		StringBuilder stringBuilder = new StringBuilder();

		// action name
		stringBuilder.append(getAction().getClass().getSimpleName());

		CtElement element = getSrcNode();

		if (element == null) {
			// some elements are only in the gumtree for having a clean diff but not in the
			// Spoon metamodel
			return stringBuilder.toString() + " fake_node(" + getAction().getNode().getMetadata("type") + ")";
		}

		// node type
		String nodeType = element.getClass().getSimpleName();
		nodeType = nodeType.substring(2, nodeType.length() - 4);
		stringBuilder.append(" ").append(nodeType);

		// action position
		CtElement parent = element;
		CtElement directParent = element;
		if (directParent.getParent() != null && !(directParent.getParent() instanceof CtPackage)) {
			directParent = directParent.getParent();
		}
		while (parent.getParent() != null && !(parent.getParent() instanceof CtPackage)) {
			parent = parent.getParent();
		}
		String position = " at ";
		if (parent instanceof CtType) {
			position += ((CtType<?>) parent).getQualifiedName();
		}
		if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
			position += ":" + element.getPosition().getLine();
		}
		position += "(";
		position += toStringPath(getSrc());
		position += ")";
		if (getAction() instanceof Move) {
			position = " from " + toStringPath(getSrc());
			position += " to " + toStringPath(getDst());
		}
		stringBuilder.append(position).append(newline);

		// code change
		String label = partialElementPrint(element);
		if (action instanceof Move) {
			label = element.toString();
		}
		if (action instanceof Update) {
			label += " to " + getDstNode().toString();
		}
		String[] split = label.split(newline);
		for (String s : split) {
			stringBuilder.append("\t").append(s).append(newline);
		}
		return stringBuilder.toString();
	}

	private static CtElement getElement(ITree node) {
		return (CtElement) node.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	}

	private static String toStringPath(ITree node) {
		// CtElement element = getElement(node);
		ITree parent = node.getParent();
		CtElement parentEle = getElement(parent);
		String str = "";
		while (parent != null && parentEle != null) {
			if (parentEle instanceof CtType) {
				str = ((CtType<?>) parentEle).getQualifiedName() + "[" + parent.getChildPosition(node) + "]" + str;
				break;
			} else if (parentEle instanceof CtExecutable) {
				str = "#" + ((CtExecutable<?>) parentEle).getSimpleName() + "[" + parent.getChildPosition(node) + "]" + str;
			} else {
				str = "[" + parent.getChildPosition(node)+ "]" + str;
			}
			node = parent;
			// element = getElement(node);
			parent = node.getParent();
			parentEle = getElement(parent);
		}

		// CtType<?> parentType = getElement(node).getParent(CtType.class);
		// CtExecutable<?> parentExe = getElement(node).getParent(CtExecutable.class);
		// if (parentExe != null) {
		// 	str += parentExe.getReference().getDeclaringType() + "#" + parentExe.getSimpleName() + "()";
		// } else if (parentType != null) {
		// 	str += parentType.getQualifiedName();
		// }

		// if (element instanceof CtType) {//TODO
		// 	str += ((CtType<?>) element).getQualifiedName();
		// 	if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
		// 		str += ":" + element.getPosition().getLine();
		// 	}
		// 	str += "(";
		// 	str += element.getLabel();
		// 	str += "[" + node.getParent().getChildPosition(node);
		// 	str += "])";
		// } else if (element instanceof CtExecutable) {//TODO
		// 	str += (element.getParent(CtType.class) != null ? element.getParent(CtType.class).getQualifiedName()
		// 			: ((CtType<?>) element).getQualifiedName());
		// 	if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
		// 		str += ":" + element.getPosition().getLine();
		// 	}
		// 	str += "(";
		// 	str += node.getParent().getLabel();
		// 	str += "[" + node.getParent().getChildPosition(node);
		// 	str += "])";
		// }
		return str;
	}

	private String partialElementPrint(CtElement element) {
		DefaultJavaPrettyPrinter print = new DefaultJavaPrettyPrinter(element.getFactory().getEnvironment()) {
			@Override
			public DefaultJavaPrettyPrinter scan(CtElement e) {
				if (e != null && e.getMetadata("isMoved") == null) {
					return super.scan(e);
				}
				return this;
			}
		};

		print.scan(element);
		return print.getResult();
	}

	/** returns the changed (inserded/deleted/updated/moved) element */
	public CtElement getSrcNode() {
		return (CtElement) getSrc().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	}

	public ITree getSrc() {
		return action.getNode();
	}

	/** 
	 * returns the new version of the node (only for update and move) 
	 * should not really be used like that
	 * better use the mapper?
	 */
	public CtElement getDstNode() {
		return null;
	}

	public ITree getDst() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getAction() == null) ? 0 : getAction().hashCode());
		result = prime * result + ((getSrcNode() == null) ? 0 : getSrcNode().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Operation other = (Operation) obj;
		if (getAction() == null) {
			if (other.getAction() != null)
				return false;
		} else if (!getAction().equals(other.getAction()))
			return false;
		if (getSrcNode() == null) {
			if (other.getSrcNode() != null)
				return false;
		} else if (!getSrcNode().equals(other.getSrcNode()))
			return false;
		return true;
	}

	public JsonElement toJson() {
		Json4SpoonGenerator x = new Json4SpoonGenerator();
		JsonObject o = new JsonObject();

		// action name
		Class<?> actionClass = getAction().getClass();
		// o.add("class0", new JsonPrimitive(aaa.toString()));
		// o.add("class01", new JsonPrimitive(aaa.toGenericString()));
		actionClass.getSimpleName(); // TODO make an issue or report the bug, result change with the number of calls
		o.add("type", new JsonPrimitive(actionClass.getSimpleName()));

		CtElement element = getSrcNode();

		if (element == null) {
			// some elements are only in the gumtree for having a clean diff but not in the Spoon metamodel
			o.add("fake_node", new JsonPrimitive(getAction().getNode().getMetadata("type").toString()));
			return o;
		}

		// node type
		String nodeType = element.getClass().getSimpleName();
		nodeType = nodeType.substring(2, nodeType.length() - 4);
		o.add("node type", new JsonPrimitive(nodeType));

		// action position
		CtElement parent = element;
		while (parent.getParent() != null && !(parent.getParent() instanceof CtPackage)) {
			parent = parent.getParent();
		}

		JsonObject curr = new JsonObject();
		if (element.getParent(CtClass.class) != null) {
			curr.add("name", new JsonPrimitive(element.getParent(CtClass.class).getQualifiedName()));
		}
		if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
			curr.add("start", new JsonPrimitive(element.getPosition().getSourceStart()));
			curr.add("end", new JsonPrimitive(element.getPosition().getSourceEnd()));
			curr.add("loc", x.positionToJson(element.getPosition()));
		}

		if (getAction() instanceof Move || getAction() instanceof Update) {
			o.add("from", curr);
			CtElement elementDest = (CtElement) getAction().getNode()
					.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
			JsonObject then = new JsonObject();
			if (elementDest.getParent(CtClass.class) != null) {
				then.add("name", new JsonPrimitive(elementDest.getParent(CtClass.class).getQualifiedName()));
			}
			if (elementDest.getPosition() != null && !(elementDest.getPosition() instanceof NoSourcePosition)) {
				then.add("start", new JsonPrimitive(elementDest.getPosition().getSourceStart()));
				then.add("end", new JsonPrimitive(elementDest.getPosition().getSourceEnd()));
				then.add("loc", x.positionToJson(elementDest.getPosition()));
			}
			if (getAction() instanceof Move) {
				// o.addProperty("value", elementDest.toString());
				o.add("valueAST", x.getJSONasJsonObject(elementDest));
				// curr.addProperty("value", element.toString());
				curr.add("valueAST", x.getJSONasJsonObject(element));
				// then.addProperty("value", elementDest.toString());
				then.add("valueAST", x.getJSONasJsonObject(elementDest));
				o.add("to", then);
			} else if (getAction() instanceof Update) {
				o.add("into", then);
				// then.addProperty("value", elementDest.toString());
				then.add("valueAST", x.getJSONasJsonObject(elementDest));
				// curr.addProperty("value", partialElementPrint(element));
				curr.add("valueAST", x.getJSONasJsonObject(element));
			}
		} else {
			// curr.addProperty("value", partialElementPrint(element));
			curr.add("valueAST", x.getJSONasJsonObject(element));
			o.add("at", curr);
		}
		return o;
	}
}

// 
// 
// 

// 
// 
// 

// 
// 
// 
// 
// 
// 
// 
