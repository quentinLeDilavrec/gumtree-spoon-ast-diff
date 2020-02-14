package gumtree.spoon;

import java.io.File;
import java.util.Base64;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import gumtree.spoon.builder.Json4SpoonGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.operations.Operation;
import spoon.SpoonModelBuilder;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

import static spark.Spark.*;

/**
 * Computes the differences between two CtElements.
 *
 * @author Matias Martinez, matias.martinez@inria.fr
 */
public class AstComparator {
	// For the moment, let's create a factory each type we get a type.
	// Sharing the factory produces a bug when asking the path of different types
	// (>1)
	// private final Factory factory;

	static {
		// default 0.3
		// it seems that default value is really bad
		// 0.1 one failing much more changes
		// 0.2 one failing much more changes
		// 0.3 one failing test_t_224542
		// 0.4 fails for issue31
		// 0.5 fails for issue31
		// 0.6 OK
		// 0.7 1 failing
		// 0.8 2 failing
		// 0.9 two failing tests with more changes
		// see GreedyBottomUpMatcher.java in Gumtree
		System.setProperty("gumtree.match.bu.sim", "0.6");

		// default 2
		// 0 is really bad for 211903 t_224542 225391 226622
		// 1 is required for t_225262 and t_213712 to pass
		System.setProperty("gumtree.match.gt.minh", "1");

		// default 1000
		// 0 fails
		// 1 fails
		// 10 fails
		// 100 OK
		// 1000 OK
		// see AbstractBottomUpMatcher#SIZE_THRESHOD in Gumtree
		// System.setProperty("gumtree.match.bu.size","10");
		// System.setProperty("gt.bum.szt", "1000");
	}
	/**
	 * By default, comments are ignored
	 */
	private boolean includeComments = false;

	public AstComparator() {
		super();
	}

	public AstComparator(boolean includeComments) {
		super();
		this.includeComments = includeComments;
	}

	public AstComparator(Map<String, String> configuration) {
		super();
		for (String k : configuration.keySet()) {
			System.setProperty(k, configuration.get(k));
		}
	}

	protected Factory createFactory() {
		Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
		factory.getEnvironment().setNoClasspath(true);
		factory.getEnvironment().setCommentEnabled(includeComments);
		return factory;
	}

	/**
	 * compares two java files
	 */
	public Diff compare(File f1, File f2) throws Exception {
		return this.compare(getCtType(f1), getCtType(f2));
	}

	/**
	 * compares two snippets
	 */
	public Diff compare(String left, String right) {
		return compare(getCtType(left), getCtType(right));
	}

	/**
	 * compares two snippets that come from the files given as argument
	 */
	public Diff compare(String left, String right, String filenameLeft, String filenameRight) {
		return compare(getCtType(left, filenameLeft), getCtType(right, filenameRight));
	}

	/**
	 * compares two AST nodes
	 */
	public Diff compare(CtElement left, CtElement right) {
		final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
		return new DiffImpl(scanner.getTreeContext(), scanner.getTree(left), scanner.getTree(right));
	}

	public CtType getCtType(File file) throws Exception {

		SpoonResource resource = SpoonResourceHelper.createResource(file);
		return getCtType(resource);
	}

	public CtType getCtType(SpoonResource resource) {
		Factory factory = createFactory();
		factory.getModel().setBuildModelIsFinished(false);
		SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
		compiler.getFactory().getEnvironment().setLevel("OFF");
		compiler.addInputSource(resource);
		compiler.build();

		if (factory.Type().getAll().size() == 0) {
			return null;
		}

		// let's first take the first type.
		CtType type = factory.Type().getAll().get(0);
		// Now, let's ask to the factory the type (which it will set up the
		// corresponding
		// package)
		return factory.Type().get(type.getQualifiedName());
	}

	public CtType<?> getCtType(String content) {
		return getCtType(content, "/test");
	}

	public CtType<?> getCtType(String content, String filename) {
		VirtualFile resource = new VirtualFile(content, filename);
		return getCtType(resource);
	}

	public static void main(String[] args) throws Exception {

		port(8087);
		get("/spoon", (req, res) -> {
			System.out.println("=========spoon=========");
			res.header("Content-Type", "application/json");
			res.header("Access-Control-Allow-Origin", "http://127.0.0.1:8080");
			res.header("Access-Control-Allow-Credentials", "true");
			String code = new String(Base64.getDecoder().decode(req.queryParams("code")));

			Json4SpoonGenerator x = new Json4SpoonGenerator();
			final Diff result = new AstComparator().compare(
				"",
				code);
			// System.out.println(result.getAllOperations().get(0));

			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			JsonObject r = x.getJSONasJsonObject(new AstComparator().getCtType(code));

			return gson.toJson(r);
		});
		get("/gumtree", (req, res) -> {
			System.out.println("=========gumtree=========");
			res.header("Content-Type", "application/json");
			res.header("Access-Control-Allow-Origin", "http://127.0.0.1:8080");
			res.header("Access-Control-Allow-Credentials", "true");
			String old = new String(Base64.getDecoder().decode(req.queryParams("old")));
			String neww = new String(Base64.getDecoder().decode(req.queryParams("new")));

			Json4SpoonGenerator x = new Json4SpoonGenerator();
			final Diff diff = new AstComparator().compare(
				old,
				neww);
			// System.out.println(old);
			// System.out.println(neww);
			// System.out.println(diff.getAllOperations().get(0));
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			// diff.getRootOperations().get(0).getAction().toString();

			JsonObject o = new JsonObject();
			// o.addProperty(JSON_PROPERTIES.label.toString(), tree.getLabel());
			// o.addProperty(JSON_PROPERTIES.type.toString(), context.getTypeLabel(tree));

			JsonArray nodeChildens = new JsonArray();
			o.add("actions", nodeChildens);

			for (Operation tch : diff.getRootOperations()) {
			    JsonObject y = new JsonObject();
				y.add("toString", new JsonPrimitive(tch.getAction().toString()));
				y.add("name", new JsonPrimitive(tch.getAction().getName()));
				y.add("class", new JsonPrimitive(tch.getAction().getClass().toString()));
				y.add("label", new JsonPrimitive(tch.getAction().getNode().getLabel()));
				y.add("srcnode", x.getJSONasJsonObject(tch.getSrcNode()));
				y.add("dstnode", x.getJSONasJsonObject(tch.getSrcNode()));
				nodeChildens.add(y);
			}
			System.out.println(diff);
			return gson.toJson(o);
		});
		// internalServerError((req, res) -> {
		// 	System.out.println("Error");
		// 	System.out.println(req);
		// 	System.out.println(res);
		// 	res.type("application/json");
		// 	return "{\"message\":\"Custom 500 handling\"}";
		// });
		exception(Exception.class, (exception, request, response) -> {
			// Handle the exception here
			System.out.println("Exception");
			System.out.println(exception);
			System.out.println(request);
			System.out.println(response);
		});
	}

}
