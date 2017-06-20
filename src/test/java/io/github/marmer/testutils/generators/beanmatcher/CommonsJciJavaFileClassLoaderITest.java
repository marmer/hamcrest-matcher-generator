package io.github.marmer.testutils.generators.beanmatcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;

import static org.junit.Assert.assertThat;


public class CommonsJciJavaFileClassLoaderITest {
	public static final String PACKAGE = "some.pck";
	private static final String CLASS_NAME = "SimpleSampleClass";
	private static final String QUALIFIED_CLASS_NAME = PACKAGE + "." + CLASS_NAME;
	private static final String JAVA_FILE_NAME = CLASS_NAME + ".java";

	private static final String VALID_JAVA_SOURCE_FILE_CONTENT = "package " + PACKAGE + ";\n" +
		"public class " + CLASS_NAME + " { }";

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	private JavaFileClassLoader classUnderTest;

	private Path sourceBaseDir;
	private Path javaFile;

	@Before
	public void setUp() throws Exception {
		initSourceBaseDir();
		initClassUnderTest();
	}

	private void initSourceBaseDir() throws Exception {
		sourceBaseDir = temp.newFolder("src").toPath();
		final Path sourceDirectory = sourceBaseDir.resolve(Paths.get(PACKAGE.replace(".", "/")));
		Files.createDirectories(sourceDirectory);
		javaFile = sourceDirectory.resolve(JAVA_FILE_NAME);
	}

	private void prepareValidSourceFile() throws IOException {
		Files.write(javaFile, VALID_JAVA_SOURCE_FILE_CONTENT.getBytes(),
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private void initClassUnderTest() throws Exception {
		classUnderTest = new CommonsJciJavaFileClassLoader(sourceBaseDir);
	}

	@Test
	public void testLoad_SourceFolderWithJavaFilesGiven_ShouldReturnCompiledClasses() throws Exception {

		// Preparation
		prepareValidSourceFile();

		// Execution
		final List<Class<?>> classLoad = classUnderTest.load(Collections.singletonList(javaFile));

		// Assertion
		assertThat("classLoad", classLoad, contains(hasProperty("name", equalTo(QUALIFIED_CLASS_NAME))));
	}
	// TODO test errors on cleass loading
	// TODO test errors on compile
	// TODO test on not existing java file
	// TODO test not existing base path

}
