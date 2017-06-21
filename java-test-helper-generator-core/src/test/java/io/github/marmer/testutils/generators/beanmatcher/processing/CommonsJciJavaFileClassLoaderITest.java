package io.github.marmer.testutils.generators.beanmatcher.processing;

import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import io.github.marmer.testutils.generators.beanmatcher.processing.CommonsJciJavaFileClassLoader;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;

import static org.junit.Assert.assertThat;


public class CommonsJciJavaFileClassLoaderITest {
	public static final String PACKAGE = "some.pck";
	private static final String CLASS_NAME = "SimpleSampleClass";
	private static final String QUALIFIED_CLASS_NAME = PACKAGE + "." + CLASS_NAME;
	private static final String JAVA_FILE_NAME = CLASS_NAME + ".java";

	private static final String VALID_JAVA_SOURCE_FILE_CONTENT = "package " + PACKAGE + ";\n" +
		"public class " + CLASS_NAME + " { }";
	private static final String INVALID_JAVA_SOURCE_FILE_CONTENT = "SomeDefinitelyNotValidJavaFileContent { }";

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

	private void prepareInvalidSourceFile() throws IOException {
		Files.write(javaFile, INVALID_JAVA_SOURCE_FILE_CONTENT.getBytes(),
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

	@Test
	public void testLoad_InvalidJavaFileGiven_ShouldNotIncludeJavafileInResults() throws Exception {

		// Preparation
		prepareInvalidSourceFile();

		// Execution
		final List<Class<?>> classLoad = classUnderTest.load(Collections.singletonList(javaFile));

		// Assertion
		assertThat("classLoad", classLoad, is(empty()));
	}

	@Test
	public void testLoad_NotExistingJavaFileGiven_ShouldNotIncludeJavafileInResults() throws Exception {

		// Preparation
		prepareValidSourceFile();
		Files.delete(javaFile);

		// Execution
		final List<Class<?>> classLoad = classUnderTest.load(Collections.singletonList(javaFile));

		// Assertion
		assertThat("classLoad", classLoad, is(empty()));
	}

	@Test
	public void testLoad_InitializedWithNotExistingBasePath_ShouldNotIncludeJavafileInResults() throws Exception {

		// Preparation
		prepareValidSourceFile();
		FileUtils.deleteQuietly(sourceBaseDir.toFile());

		// Execution
		final List<Class<?>> classLoad = classUnderTest.load(Collections.singletonList(javaFile));

		// Assertion
		assertThat("classLoad", classLoad, is(empty()));
	}

}
