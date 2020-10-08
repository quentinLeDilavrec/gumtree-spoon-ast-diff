package gumtree.spoon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

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

public class ExamplesApplyTest {

    @Before
    public void initProps() {
        System.setProperty("nolabel", "true");
	}
	
	@Test
	public void test1() throws Exception {
		// File fl = new File("src/test/resources/examples/roots/test8/left_QuickNotepad_1.13.java");
		File examples = new File("src/test/resources/examples");
		
		for (File f : FileUtils.listFiles(examples, new String[] { "java" }, true)) {
			ApplyTestHelper.onInsert(f);
		}
	}

}
