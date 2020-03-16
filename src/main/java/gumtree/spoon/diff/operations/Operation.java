package gumtree.spoon.diff.operations;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import gumtree.spoon.builder.Json4SpoonGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

public abstract class Operation<T extends Action> {
	private final CtElement node;
	private final T action;

	public Operation(T action) {
		this.action = action;
		this.node = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
	}

	/** use {@link #getSrcNode()} or {@link #getDstNode()} instead. */
	@Deprecated
	public CtElement getNode() {
		return node;
	}

	public T getAction() {
		return action;
	}

	@Override
	public String toString() {
		return toStringAction(action);
	}

	private String toStringAction(Action action) {
		String newline = System.getProperty("line.separator");
		StringBuilder stringBuilder = new StringBuilder();

		// action name
		stringBuilder.append(action.getClass().getSimpleName());

		CtElement element = node;

		if (element == null) {
			// some elements are only in the gumtree for having a clean diff but not in the
			// Spoon metamodel
			return stringBuilder.toString() + " fake_node(" + action.getNode().getMetadata("type") + ")";
		}

		// node type
		String nodeType = element.getClass().getSimpleName();
		nodeType = nodeType.substring(2, nodeType.length() - 4);
		stringBuilder.append(" ").append(nodeType);

		// action position
		CtElement parent = element;
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
		if (action instanceof Move) {
			CtElement elementDest = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
			position = " from " + element.getParent(CtClass.class).getQualifiedName();
			if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
				position += ":" + element.getPosition().getLine();
			}
			position += " to " + elementDest.getParent(CtClass.class).getQualifiedName();
			if (elementDest.getPosition() != null && !(elementDest.getPosition() instanceof NoSourcePosition)) {
				position += ":" + elementDest.getPosition().getLine();
			}
		}
		stringBuilder.append(position).append(newline);

		// code change
		String label = partialElementPrint(element);
		if (action instanceof Move) {
			label = element.toString();
		}
		if (action instanceof Update) {
			CtElement elementDest = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
			label += " to " + elementDest.toString();
		}
		String[] split = label.split(newline);
		for (String s : split) {
			stringBuilder.append("\t").append(s).append(newline);
		}
		return stringBuilder.toString();
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

	/** returns the changed (inserded/deleted/updated) element */
	public CtElement getSrcNode() {
		return node;
	}

	/** returns the new version of the node (only for update) */
	public CtElement getDstNode() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((node == null) ? 0 : node.hashCode());
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
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}

	public JsonElement toJson() {
		Json4SpoonGenerator x = new Json4SpoonGenerator();
		JsonObject o = new JsonObject();

		// action name
		Class<?> actionClass = this.action.getClass();
		// o.add("class0", new JsonPrimitive(aaa.toString()));
		// o.add("class01", new JsonPrimitive(aaa.toGenericString()));
		actionClass.getSimpleName(); // TODO make an issue or report the bug, result change with the number of calls
		o.add("type", new JsonPrimitive(actionClass.getSimpleName()));

		CtElement element = this.node;

		if (element == null) {
			// some elements are only in the gumtree for having a clean diff but not in the Spoon metamodel
			o.add("fake_node", new JsonPrimitive(this.action.getNode().getMetadata("type").toString()));
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
		if (element.getParent(CtClass.class)!=null) {
			curr.add("name", new JsonPrimitive(element.getParent(CtClass.class).getQualifiedName()));
		}
		if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
			curr.add("start", new JsonPrimitive(element.getPosition().getSourceStart()));
			curr.add("end", new JsonPrimitive(element.getPosition().getSourceEnd()));
			curr.add("loc", x.positionToJson(element.getPosition()));
		}

		if (this.action instanceof Move || this.action instanceof Update) {
			o.add("from", curr);
			CtElement elementDest = (CtElement) this.action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
			JsonObject then = new JsonObject();
			if (elementDest.getParent(CtClass.class)!=null) {
				then.add("name", new JsonPrimitive(elementDest.getParent(CtClass.class).getQualifiedName()));
			}
			if (elementDest.getPosition() != null && !(elementDest.getPosition() instanceof NoSourcePosition)) {
				then.add("start", new JsonPrimitive(elementDest.getPosition().getSourceStart()));
				then.add("end", new JsonPrimitive(elementDest.getPosition().getSourceEnd()));
				then.add("loc", x.positionToJson(elementDest.getPosition()));
			}
			if (this.action instanceof Move) {
				// o.addProperty("value", elementDest.toString());
				o.add("valueAST", x.getJSONasJsonObject(elementDest));
				// curr.addProperty("value", element.toString());
				curr.add("valueAST", x.getJSONasJsonObject(element));
				// then.addProperty("value", elementDest.toString());
				then.add("valueAST", x.getJSONasJsonObject(elementDest));
				o.add("to", then);
			} else if(this.action instanceof Update) {
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
	 

	 
	
	
	
	 
	

	
	
	
	
