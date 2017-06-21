package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.utils.matchers.GeneratedFileCompiler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import java.util.Arrays;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;


public class JavaPoetFactoryMethodFacadeGeneratorITest {
	private FactoryMethodFacadeGenerator classUnderTest;
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	private GeneratedFileCompiler compiler;
	private Path classOutputDir;
	private Path srcOutputDir;

	@Before
	public void setUp() throws Exception {
		prepareOutputDir();
		prepareSourceOutputDir();
		initCompiler();
		initClassUnderTest();
	}

	public void prepareOutputDir() throws Exception {
		this.classOutputDir = temp.newFolder("target").toPath();
	}

	public void prepareSourceOutputDir() throws Exception {
		this.srcOutputDir = temp.newFolder("src").toPath();
	}

	private void initCompiler() {
		compiler = new GeneratedFileCompiler(srcOutputDir, classOutputDir);
	}

	private void initClassUnderTest() throws Exception {
		classUnderTest = new JavaPoetFactoryMethodFacadeGenerator(srcOutputDir,
				GeneratedFileCompiler.FACADE_PACKAGE, GeneratedFileCompiler.FACADE_NAME);
	}

	@Test
	public void testGenerateFacadeFor_MultipleClassesGiven_ResultingMatcherClassShouldHaveStaticMethodsReturningInstancesOfGivenClasses()
		throws Exception {
		// Preparation

		// Execution
		classUnderTest.generateFacadeFor(Arrays.asList(Sample1Type.class, Sample2Type.class));

		// Assertion
		final Class<?> fascade = compiler.compileAndLoadFacade();
		assertThat("Declared Methods of generated Fascade", fascade.getDeclaredMethods(),
			is(allOf(hasItemInArray(hasProperty("name", equalTo("isSample1Type"))),
					hasItemInArray(hasProperty("name", equalTo("isSample2Type"))))));
	}

	// TODO methods are static
	// TODO test crestedInstancesShouldNotBeNewAndNotRecycled
	// TODO test ClassHasNoDefaultConstructor
	// TODO test Should have generated annotation
	// TODO test should be util class (is final and has no non private constructor)

	public static class Sample1Type { }

	public static class Sample2Type { }
}
