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
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.Version.COMP_RES;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gumtree.spoon.builder.Json4SpoonGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.MultiDiffImpl;
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
		// System.setProperty("gumtree.match.gt.ag.nomove", "true");

	}
	/**
	 * By default, comments are ignored
	 */
	private boolean includeComments = false;

	private TreeContext context;
	public TreeContext getContext() {
		return context;
	}

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
		SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
		this.context = scanner.getTreeContext();
		final Version1 rightV = new Version1();
		final Version leftV = new Version() {
			@Override
			public COMP_RES partiallyCompareTo(Version other) {
				return other == this ? Version.COMP_RES.EQUAL
						: (other == rightV ? Version.COMP_RES.SUPERIOR : Version.COMP_RES.UNKNOWN);
			}
		};
		rightV.other = leftV;
		MultiDiffImpl r = new MultiDiffImpl(scanner.getTree(left), leftV);
		return r.compute(scanner.getTree(right), rightV);
	}

	public Diff compare(CtElement... versions) {
		return null; // TODO
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
		// diff.getRootOperations().get(0).getAction().toString();

		JsonObject o = new JsonObject();
		// o.addProperty(JSON_PROPERTIES.label.toString(), tree.getLabel());
		// o.addProperty(JSON_PROPERTIES.type.toString(), context.getTypeLabel(tree));

		JsonArray nodeChildens = new JsonArray();
		o.add("actions", nodeChildens);

		for (Operation tch : ops) {
			// JsonObject y = new JsonObject();
			// System.err.println(7);
			// y.add("toString", new JsonPrimitive(tch.toString()));
			// System.out.println(tch.toString());
			// y.add("toJson", tch.toJson());
			nodeChildens.add(tch.toJson());
			// // y.add("name", new JsonPrimitive(tch.getAction().getName()));
			// // y.add("class", new
			// // JsonPrimitive(tch.getAction().getClass().getSimpleName()));
			// // y.add("label", new JsonPrimitive(tch.getAction().getNode().getLabel()));
			// // y.add("srcnode", x.getJSONasJsonObject(tch.getSrcNode()));
			// // y.add("dstnode", x.getJSONasJsonObject(tch.getSrcNode()));
			// nodeChildens.add(y);
		}
		return o;
	}

	private static final class Version1 implements Version {
		public Version other;

		@Override
		public COMP_RES partiallyCompareTo(Version other) {
			if (other == this) {
				return Version.COMP_RES.EQUAL;
			}
			return other == this ? Version.COMP_RES.EQUAL
					: (other == this.other ? Version.COMP_RES.INFERIOR : Version.COMP_RES.UNKNOWN);
		}
	}

	public static void main(String[] args) throws Exception {
		Server.server(args);
	}

}
