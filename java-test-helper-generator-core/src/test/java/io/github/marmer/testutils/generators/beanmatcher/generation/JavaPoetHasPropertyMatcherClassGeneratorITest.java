package io.github.marmer.testutils.generators.beanmatcher.generation;

import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.dependencies.BasedOn;
import io.github.marmer.testutils.generators.beanmatcher.processing.BeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.IntrospektorBeanPropertyExtractor;
import io.github.marmer.testutils.utils.matchers.GeneratedFileCompiler;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.lang3.reflect.MethodUtils;

import org.hamcrest.Matcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Method;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.marmer.testutils.utils.matchers.CleanCompilationResultMatcher.hasNoErrorsOrWarnings;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;

import static org.hamcrest.io.FileMatchers.anExistingFile;

import static org.junit.Assert.assertThat;


public class JavaPoetHasPropertyMatcherClassGeneratorITest {
	private static final String MATCHER_POSTFIX = "Matcher";
	private final BeanPropertyExtractor propertyExtractor = new IntrospektorBeanPropertyExtractor();
	private HasPropertyMatcherClassGenerator classUnderTest;
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
		classUnderTest = new JavaPoetHasPropertyMatcherClassGenerator(
				propertyExtractor, srcOutputDir);
	}

	private void initCompiler() {
		compiler = new GeneratedFileCompiler(srcOutputDir, classOutputDir) {

			@Override
			public String getGeneratedMatcherClassNameFor(final Class<?> type) {
				return type.getSimpleName() + MATCHER_POSTFIX;
			}
		};
	}

	public void prepareOutputDir() throws Exception {
		this.classOutputDir = temp.newFolder("target").toPath();
	}

	public void prepareSourceOutputDir() throws Exception {
		this.srcOutputDir = temp.newFolder("src").toPath();
	}

	@Test
	public void testGenerateMatcherFor_SimplePojoClassGiven_ShouldCreateJavaFile() throws Exception {

		// Preparation
		classUnderTest.generateMatcherFor(SimplePojo.class);

		// Assertion
		assertThat(generatedSourceFileFor(SimplePojo.class), is(anExistingFile()));
	}

	@Test
	public void testGenerateMatcherFor_SimplePojoClassGiven_ShouldReturnPathOfCreatedJavaFile() throws Exception {
		// Preparation

		// Execution
		final Path pathOfGeneratedMatcher = classUnderTest.generateMatcherFor(SimplePojo.class);

		// Assertion
		assertThat("pathOfGeneratedMatcher", pathOfGeneratedMatcher,
			is(equalTo(compiler.getGeneratedSourcePathFor(SimplePojo.class))));
	}

	@Test
	public void testGenerateMatcherFor_FileHasBeanCreated_CreatedJavaFileShouldBeCompilableWithoutAnyIssues()
		throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type);

		// Assertion
		final CompilationResult result = compiler.compileGeneratedSourceFileFor(type);
		assertThat(result, hasNoErrorsOrWarnings());
	}

	@Test
	public void testGenerateMatcherFor_FileHasBeanCreated_ShouldBeAbleToLoadAndInstanciateGeneratedClass()
		throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type);

		// Assertion
		compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
	}

	@Test
	public void testGenerateMatcherFor_MatcherHasBeenGenerated_ShouldBeAnnotatedWithBaseClass() throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type);

		// Assertion
		final Class<?> generatedClass = compiler.compileAndLoadGeneratedClassFor(type);
		final BasedOn annotation = generatedClass.getAnnotation(BasedOn.class);
		assertThat("Annotation value of generated class", annotation.value(), is(type));
	}

	@Test
	public void testGenerateMatcherFor_InstanceOfGeneratedMatcherHasBeenCreated_GeneratedInstanceCanBeUsedToMatchRelatedInstances()
		throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);

		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);

		// Execution
		final boolean matches = matcher.matches(new SimplePojo("someValue"));

		// Assertion
		assertThat("Matches on same Instance", matches, is(true));
	}

	@Test
	public void testGenerateMatcherFor_MatcherHasBeenCreated_GeneratedTypeIsAnnotatedWithGenerated() throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type);

		// Assertion
		final List<String> sourceFileLines = readGeneratedSourceFileLines();
		assertThat("Generated source file lines", sourceFileLines,
			hasGeneratedAnnotationBeforeGeneratedClassDefinitionFor(type));
	}

	@Test
	public void testGenerateMatcherFor_MatcherHasBeenCreated_GeneratedTypeShouldHaveAMethodPerPropertyWhichTakesAnotherMatcher()
		throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);
		compiler.compileGeneratedSourceFileFor(type);

		// Execution
		final Class<?> generatedMatcherClass = compiler.loadGeneratedClassFor(type);

		// Assertion
		assertThat("Declared matcher methods: ", nonSyntheticMethodsOf(generatedMatcherClass),
			hasItem(
				is(matcherConsumingMethodWithReturntypeAndName(generatedMatcherClass, "withSimpleProp"))));
	}

	@Test
	public void testGenerateMatcherFor_MatcherHasBeenCreatedWithChildTypeAndPropertiesAtParent_GeneratedTypeShouldHaveAMethodPerPropertyWhichTakesAnotherMatcher()
		throws Exception {

		// Preparation
		final Class<SimplePojoChild> type = SimplePojoChild.class;
		classUnderTest.generateMatcherFor(type);
		compiler.compileGeneratedSourceFileFor(type);

		// Execution
		final Class<?> generatedMatcherClass = compiler.loadGeneratedClassFor(type);

		// Assertion
		assertThat("Declared matcher methods: ", nonSyntheticMethodsOf(generatedMatcherClass),
			hasItem(
				is(matcherConsumingMethodWithReturntypeAndName(generatedMatcherClass, "withSimpleProp"))));
	}

	@Test
	public void testGenerateMatcherFor_GeneratedInstanceHasMatcherSetAndNotMatchingValueIsGiven_ShouldNotMatch()
		throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);
		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		MethodUtils.invokeMethod(matcher, "withSimpleProp", equalTo("someValue"));

		// Execution
		final boolean matches = matcher.matches(new SimplePojo("someOtherValue"));

		// Assertion
		assertThat("Matcher matches matching class", matches, is(false));
	}

	@Test
	public void testGenerateMatcherFor_GeneratedInstanceHasMatcherSetAndMatchingValueIsGiven_ShouldMatch()
		throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);
		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		MethodUtils.invokeMethod(matcher, "withSimpleProp", equalTo("someValue"));

		// Execution
		final boolean matches = matcher.matches(new SimplePojo("someValue"));

		// Assertion
		assertThat("Matcher matches matching class", matches, is(true));
	}

	@Test
	public void testGenerateMatcherFor_GeneratedInstanceMatcherSettingMethodIsCalled_MethodShouldReturnIstanceOfItselfForConcatenationAbility()
		throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);
		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);

		// Execution
		final Object result = MethodUtils.invokeMethod(matcher, "withSimpleProp", equalTo("someValue"));

		// Assertion
		assertThat(result, is(sameInstance(matcher)));
	}

	private Collection<Method> nonSyntheticMethodsOf(final Class<?> generatedMatcherClass) {
		return Arrays.stream(generatedMatcherClass.getDeclaredMethods()).filter(m -> !m.isSynthetic()).collect(
				Collectors
					.toList());
	}

	private Matcher<Method> matcherConsumingMethodWithReturntypeAndName(
		final Class<?> generatedMatcherClass,
		final String propertyName) {
		return allOf(
				hasProperty("name", equalTo(propertyName)),
				hasProperty("parameterTypes", arrayContaining(Matcher.class)),
				hasProperty("returnType", is(generatedMatcherClass)));
	}

	private Matcher<Iterable<? extends String>> hasGeneratedAnnotationBeforeGeneratedClassDefinitionFor(
		final Class<SimplePojo> type) {
		return containsInRelativeOrder(startsWith(
					"@Generated(\"" + JavaPoetHasPropertyMatcherClassGenerator.class.getName() + "\")"),
				containsString("class " + compiler.getGeneratedMatcherClassNameFor(type)));
	}

	private List<String> readGeneratedSourceFileLines() throws IOException {
		return Files.readAllLines(generatedSourceFileFor(SimplePojo.class).toPath());
	}

	private File generatedSourceFileFor(final Class<?> type) {
		return compiler.getGeneratedSourcePathFor(type).toFile();
	}

	public static class SimplePojo {
		private final String simpleProp;

		public SimplePojo(final String simpleProp) {
			this.simpleProp = simpleProp;
		}

		public String anotherNonPropertyMethod() {
			return "someNonPropertyValue";
		}

		public String getSimpleProp() {
			return simpleProp;
		}
	}

	public static class SimplePojoChild extends SimplePojo {
		public SimplePojoChild(final String simpleProp) {
			super(simpleProp);
		}
	}

}
