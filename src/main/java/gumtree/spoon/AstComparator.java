package gumtree.spoon;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.gumtreediff.actions.model.Insert;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

	public JsonElement formatDiff(List<Operation> ops) {

		Json4SpoonGenerator x = new Json4SpoonGenerator();
		// diff.getRootOperations().get(0).getAction().toString();

		JsonObject o = new JsonObject();
		// o.addProperty(JSON_PROPERTIES.label.toString(), tree.getLabel());
		// o.addProperty(JSON_PROPERTIES.type.toString(), context.getTypeLabel(tree));

		JsonArray nodeChildens = new JsonArray();
		o.add("actions", nodeChildens);

		for (Operation tch : ops) {
			JsonObject y = new JsonObject();
			y.add("toString", new JsonPrimitive(tch.toString()));
			// System.out.println(tch.toString());
			y.add("toJson", tch.toJson());
			// y.add("name", new JsonPrimitive(tch.getAction().getName()));
			// y.add("class", new
			// JsonPrimitive(tch.getAction().getClass().getSimpleName()));
			// y.add("label", new JsonPrimitive(tch.getAction().getNode().getLabel()));
			// y.add("srcnode", x.getJSONasJsonObject(tch.getSrcNode()));
			// y.add("dstnode", x.getJSONasJsonObject(tch.getSrcNode()));
			nodeChildens.add(y);
		}
		return o;
	}

	public static void main(String[] args) throws Exception {
		Set<String> validOrigins = new HashSet<String>();
		validOrigins.add("http://127.0.0.1:8081");
		validOrigins.add("http://localhost:8081");
		int serverport = 8087;
		AstComparator comp = new AstComparator();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Json4SpoonGenerator x = new Json4SpoonGenerator();
		if (args.length <= 1 || Objects.equals(args[0], "server")) {
			if ((args.length == 1 && !Objects.equals(args[0], "server")) || args.length > 1) {
				serverport = Integer.valueOf(args[1]);
			}
			System.out.println("Launching server on " + serverport);
			port(serverport);
			get("/spoon", (req, res) -> {
				System.out.println("=========spoon=========");
				res.header("Content-Type", "application/json");
				if (validOrigins.contains(req.headers("Origin"))) {
					res.header("Access-Control-Allow-Origin", req.headers("Origin"));
				}
				res.header("Access-Control-Allow-Credentials", "true");
			
				// // Make sure it's a valid origin
				// if (validOrigins. (req.origin) != -1) {
				// 	// Set the header to the requested origin 
				// 	res.header('Access-Control-Allow-Origin', origin);
				// }      
				// // res.header("Access-Control-Allow-Origin", "http://127.0.0.1:8080"); // TODO accept more than one origin
				String code = new String(Base64.getDecoder().decode(req.queryParams("code")));

				JsonObject r = x.getJSONasJsonObject(comp.getCtType(code));

				return gson.toJson(r);
			});
			get("/gumtree", (req, res) -> {
				System.out.println("=========gumtree=========");
				res.header("Content-Type", "application/json");
				if (validOrigins.contains(req.headers("Origin"))) {
					res.header("Access-Control-Allow-Origin", req.headers("Origin"));
				}
				res.header("Access-Control-Allow-Credentials", "true");
				String old = new String(Base64.getDecoder().decode(req.queryParams("old")));
				String neww = new String(Base64.getDecoder().decode(req.queryParams("new")));

				final Diff diff = comp.compare(old, neww);
				// System.out.println(old);
				// System.out.println(neww);
				// System.out.println(diff.getAllOperations().get(0));
				JsonElement o = comp.formatDiff(diff.getRootOperations());
				System.out.println(o);
				return gson.toJson(o);
			});
			// internalServerError((req, res) -> {
			// System.out.println("Error");
			// System.out.println(req);
			// System.out.println(res);
			// res.type("application/json");
			// return "{\"message\":\"Custom 500 handling\"}";
			// });
			exception(Exception.class, (exception, request, response) -> {
				// Handle the exception here
				System.out.println("Exception");
				System.out.println(exception);
				System.out.println(request);
				System.out.println(response);
			});
		} else if(Objects.equals(args[0],"cli")){
			File fl = new File(args[1]);
			if (args.length==2) {

				JsonObject r = x.getJSONasJsonObject(comp.getCtType(fl));
				System.out.println(gson.toJson(r));
			} else {
				File fr = new File(args[2]);

				final Diff diff = comp.compare(fl, fr);
				// System.out.println(diff);
				// System.out.println(neww);
				// System.out.println(diff.getAllOperations().get(0));
				List<Operation> ops = new ArrayList<Operation>();
				for (Operation tch : diff.getRootOperations()) {
					if (tch.getAction() instanceof Insert) {
						ops.add(tch);					
					}
				}
				JsonElement o = comp.formatDiff(ops);
				// System.out.println(gson.toJson(o.getAsJsonObject().get("actions").getAsJsonArray().get(0)));
				System.out.println(gson.toJson(o));
			}
		}
	}

}






