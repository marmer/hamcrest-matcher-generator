package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetFactoryMethodFacadeGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGeneratorITest;
import io.github.marmer.testutils.generators.beanmatcher.MatcherFileGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGeneratorITest.SimplePojo;
import io.github.marmer.testutils.generators.beanmatcher.processing.BeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.CommonsJciJavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.IntrospektorBeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.PotentialPojoClassFinder;
import io.github.marmer.testutils.generators.beanmatcher.processing.ReflectionPotentialBeanClassFinder;
import io.github.marmer.testutils.utils.matchers.GeneratedFileCompiler;

import org.apache.commons.lang3.reflect.MethodUtils;

import org.hamcrest.Matcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import sample.classes.SimpleSampleClass;

import java.lang.reflect.Method;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.sameInstance;

import static org.junit.Assert.assertThat;


public class MatcherFileGeneratorITest {

	private PotentialPojoClassFinder potentialPojoClassFinder;
	private TestHelperMatcherGenerator classUnderTest;
	private HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator;

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();
	private Path srcOutputDir;
	private Path classOutputDir;
	private GeneratedFileCompiler compiler;

	@Before
	public void setUp() throws Exception {
		prepareSourceOutputDir();
		prepareOutputDir();
		initCompiler();
		initClassUnderTest();
	}

	private void initClassUnderTest() {
		potentialPojoClassFinder = new ReflectionPotentialBeanClassFinder();
		final BeanPropertyExtractor propertyExtractor = new IntrospektorBeanPropertyExtractor();
		hasPropertyMatcherClassGenerator = new JavaPoetHasPropertyMatcherClassGenerator(propertyExtractor,
				srcOutputDir);
		classUnderTest = new MatcherFileGenerator(potentialPojoClassFinder, hasPropertyMatcherClassGenerator,
				new JavaPoetFactoryMethodFacadeGenerator(srcOutputDir, GeneratedFileCompiler.FACADE_PACKAGE,
					GeneratedFileCompiler.FACADE_NAME),
				new CommonsJciJavaFileClassLoader(srcOutputDir));
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

	@Test
	public void testGenerateHelperForClassesAllIn_GeneratedInstanceHasMatcherSetAndNotMatchingValueIsGiven_ShouldNotMatch()
		throws Exception {

		// Preparation
		final Class<SimpleSampleClass> type = SimpleSampleClass.class;
		classUnderTest.generateHelperForClassesAllIn(type.getName());
		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		MethodUtils.invokeMethod(matcher, "withSomeProperty", equalTo("someValue"));

		// Execution
		final boolean matches = matcher.matches(new SimpleSampleClass("someOtherValue"));

		// Assertion
		assertThat("Matcher matches matching class", matches, is(false));
	}

	@Test
	public void testGenerateHelperForClassesAllIn_GeneratedInstanceHasMatcherSetAndMatchingValueIsGiven_ShouldMatch()
		throws Exception {

		// Preparation
		final Class<SimpleSampleClass> type = SimpleSampleClass.class;
		classUnderTest.generateHelperForClassesAllIn(type.getName());
		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		MethodUtils.invokeMethod(matcher, "withSomeProperty", equalTo("someValue"));

		// Execution
		final boolean matches = matcher.matches(new SimpleSampleClass("someValue"));

		// Assertion
		assertThat("Matcher matches matching class", matches, is(true));
	}

	@Test
	public void testGenerateMatcherFor_GeneratedInstanceMatcherSettingMethodIsCalled_MethodShouldReturnIstanceOfItselfForConcatenationAbility()
		throws Exception {

		// Preparation
		final Class<?> type = SimpleSampleClass.class;
		classUnderTest.generateHelperForClassesAllIn(type.getName());
		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);

		// Execution
		final Object result = MethodUtils.invokeMethod(matcher, "withSomeProperty", equalTo("someValue"));

		// Assertion
		assertThat(result, is(sameInstance(matcher)));
	}

	@Test
	public void testGenerateMatcherFor_PackageGiven_FassadeWithFactoryMethodsForGeneratedMatchesExists()
		throws Exception {

		// Preparation
		final Class<?> type = SimpleSampleClass.class;
		classUnderTest.generateHelperForClassesAllIn(type.getName());

		// Execution
		classUnderTest.generateHelperForClassesAllIn(type.getName());

		// Assertion
		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		final Method facadeMethod = compiler.getFacadeMethodFor(type);
		assertThat("Return Type of matcher facade factory method for " + type, facadeMethod.getReturnType().getName(),
			is(equalTo(matcher.getClass().getName())));
	}
}
