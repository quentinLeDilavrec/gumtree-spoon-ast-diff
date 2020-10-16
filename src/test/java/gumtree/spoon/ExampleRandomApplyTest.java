package gumtree.spoon;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExampleRandomApplyTest {

	private File left;
	private File right;

	public ExampleRandomApplyTest(File left, File right) {
		this.left = left;
		this.right = right;
	}

        @Parameters(name = "{index}: {0} {1}")
        public static Collection<File[]> data() {
                File examples = new File("src/test/resources/examples");

                Collection<File[]> data = new ArrayList<File[]>();
                File prev = null; 
                for (File f : FileUtils.listFiles(examples, new String[] { "java" }, true)) {
                        if (prev!=null) {
                                data.add(new File[] { prev, f });
                        }
                        prev = f;
                }

                return data;
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