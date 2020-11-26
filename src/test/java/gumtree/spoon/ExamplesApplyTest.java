package gumtree.spoon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.io.TreeIoUtils.TreeSerializer;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.gson.JsonObject;

import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.builder.Json4SpoonGenerator;
import gumtree.spoon.builder.Json4SpoonGenerator.JSON_PROPERTIES;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.Launcher;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.path.CtPath;
import spoon.reflect.visitor.CtScanner;

@RunWith(Parameterized.class)
public class ExamplesApplyTest {

	private File left;
	private File right;

	public ExamplesApplyTest(File left, File right) {
		this.left = left;
		this.right = right;
	}

	@Parameters(name = "{index}: {0} {1}")
	public static Collection<File[]> data() {
		File examples = new File("src/test/resources/examples");

		List<File[]> data = new ArrayList<File[]>();

		aux(data, examples);

		return data;
	}

	private static void aux(Collection<File[]> data, File d) {
		File b = null;
		for (File f : d.listFiles()) {
			if (f.isDirectory()) {
				aux(data,f);
			} else if (b!=null) {
				data.add(new File[] { b, f });
			} else if (f.getName().endsWith(".java")) {
				b = f;
			}
		}
	}

	@Before
	public void initProps() {
		System.setProperty("nolabel", "true");
	}

	@Test
	public void test1() throws Exception {
		ApplyTestHelper.onChange(this.left, this.right);
	}

	@Test
	public void test2() throws Exception {
		ApplyTestHelper.onChange(this.right, this.left);
	}

}
