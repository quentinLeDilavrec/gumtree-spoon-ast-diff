package gumtree.spoon.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import gumtree.spoon.builder.jsonsupport.NodePainter;
import gumtree.spoon.builder.jsonsupport.OperationNodePainter;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

/**
 *
 * Creates a JSON representation of a Spoon Java abstract syntaxt tree.
 *
 * @author Matias Martinez
 *
 */
public class Json4SpoonGenerator {

	public enum JSON_PROPERTIES {
		label, type, op, children;
	};

	@SuppressWarnings("rawtypes")
	public JsonObject getJSONasJsonObject(CtElement element) {
		SpoonGumTreeBuilder builder = new SpoonGumTreeBuilder();
		ITree generatedTree = builder.getTree(element);

		TreeContext tcontext = builder.getTreeContext();
		return this.getJSONasJsonObject(tcontext, generatedTree);
	}

	public String getJSONasString(CtElement element) {
		SpoonGumTreeBuilder builder = new SpoonGumTreeBuilder();
		ITree generatedTree = builder.getTree(element);

		TreeContext tcontext = builder.getTreeContext();

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.getJSONasJsonObject(tcontext, generatedTree)) + "\n";
	}

	public JsonObject getJSONasJsonObject(TreeContext context, ITree tree) {
		JsonObject o = new JsonObject();
		o.addProperty(JSON_PROPERTIES.label.toString(), tree.getLabel());
		o.addProperty(JSON_PROPERTIES.type.toString(), context.getTypeLabel(tree));
		CtElement e = (CtElement) tree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		if (e != null) {
			SourcePosition p = e.getPosition();
			try {
				o.add("loc", positionToJson(p));
				o.addProperty("start", p.getSourceStart());
				o.addProperty("end", p.getSourceEnd());
			} catch (Exception ee) {
				System.out.println(p);
				System.out.println(ee);
			}
		}

		JsonArray nodeChildens = new JsonArray();
		o.add(JSON_PROPERTIES.children.toString(), nodeChildens);

		for (ITree tch : tree.getChildren()) {
			JsonObject childJSon = getJSONasJsonObject(context, tch);
			if (childJSon != null)
				nodeChildens.add(childJSon);
		}
		return o;
	}

	/**
	 * Decorates a node with the affected operator, if any.
	 * 
	 * @param context
	 * @param tree
	 * @param operations
	 * @return
	 */
	public JsonObject getJSONwithOperations(TreeContext context, ITree tree, List<Operation> operations) {

		OperationNodePainter opNodePainter = new OperationNodePainter(operations);
		Collection<NodePainter> painters = new ArrayList<NodePainter>();
		painters.add(opNodePainter);
		return getJSONwithCustorLabels(context, tree, painters);
	}

	@SuppressWarnings("unused")
	public JsonObject getJSONwithCustorLabels(TreeContext context, ITree tree, Collection<NodePainter> nodePainters) {

		JsonObject o = new JsonObject();
		o.addProperty(JSON_PROPERTIES.label.toString(), tree.getLabel());
		o.addProperty(JSON_PROPERTIES.type.toString(), context.getTypeLabel(tree));
		for (NodePainter nodePainter : nodePainters) {
			nodePainter.paint(tree, o);
		}

		JsonArray nodeChildens = new JsonArray();
		o.add(JSON_PROPERTIES.children.toString(), nodeChildens);

		for (ITree tch : tree.getChildren()) {
			JsonObject childJSon = getJSONwithCustorLabels(context, tch, nodePainters);
			if (childJSon != null)
				nodeChildens.add(childJSon);
		}
		return o;

	}

	public JsonElement positionToJson(SourcePosition pos) {
		JsonObject loc = new JsonObject();
		JsonObject start = new JsonObject();
		JsonObject end = new JsonObject();
		loc.add("start", start);
		start.addProperty("line", pos.getLine());
		start.addProperty("column", pos.getColumn());
		loc.add("end", end);
		end.addProperty("line", pos.getEndLine());
		end.addProperty("column", pos.getEndColumn());
		return loc;
	}

}
