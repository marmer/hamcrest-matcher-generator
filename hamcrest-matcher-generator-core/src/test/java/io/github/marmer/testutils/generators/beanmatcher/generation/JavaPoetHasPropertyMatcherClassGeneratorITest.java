package io.github.marmer.testutils.generators.beanmatcher.generation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BasedOn;
import io.github.marmer.testutils.generators.beanmatcher.processing.BeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.IntrospektorBeanPropertyExtractor;
import io.github.marmer.testutils.utils.matchers.GeneratedFileCompiler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import sample2.classes.ClassWithoutAnyProperty;
import sample2.classes.PojoWithMatcherProperty;
import sample2.classes.SimplePojo;
import sample2.classes.SimplePojoChild;

import javax.annotation.Generated;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.marmer.testutils.utils.matchers.CleanCompilationResultMatcher.hasNoErrorsOrWarnings;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.assertThat;

public class JavaPoetHasPropertyMatcherClassGeneratorITest {
	private static final String MATCHER_POSTFIX = "Matcher";
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();
	private final BeanPropertyExtractor propertyExtractor = new IntrospektorBeanPropertyExtractor();
	private HasPropertyMatcherClassGenerator classUnderTest;
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
		classUnderTest = new JavaPoetHasPropertyMatcherClassGenerator(propertyExtractor, srcOutputDir, new PlainMatcherNamingStrategy());
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
	public void testGenerateMatcherFor_SimplePojoClassGivenAndFileAllreadyExists_ShouldCreateJavaFile()
			throws Exception {
		// Preparation
		createEmptyFileFor(SimplePojo.class);

		// Preparation
		classUnderTest.generateMatcherFor(SimplePojo.class);

		// Assertion
		assertThat(generatedSourceFileFor(SimplePojo.class), is(anExistingFile()));
	}

	private void createEmptyFileFor(final Class<SimplePojo> type) throws IOException {
		final File f = generatedSourceFileFor(type);
		f.getParentFile().mkdirs();
		f.createNewFile();
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
	public void testGenerateMatcherFor_SomeClassWithoutAnyPropertyWithoutIgnoringSuchClasses_ShouldCreateJavaFile()
			throws Exception {

		// Preparation
		classUnderTest.generateMatcherFor(ClassWithoutAnyProperty.class);

		// Assertion
		assertThat(generatedSourceFileFor(ClassWithoutAnyProperty.class), is(anExistingFile()));
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
		final Class<ClassWithoutAnyProperty> type = ClassWithoutAnyProperty.class;
		classUnderTest.generateMatcherFor(type);

		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);

		// Execution
		final boolean matches = matcher.matches(new ClassWithoutAnyProperty());

		// Assertion
		assertThat("Matches on same Instance", matches, is(true));
	}

	@Test
	public void testGenerateMatcherFor_MatcherHasBeenCreated_GeneratedAnnotatedIsImported() throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type);

		// Assertion
		final String source = readGeneratedSourceFileLines();
		final CompilationUnit javaFile = JavaParser.parse(source);
		assertThat(javaFile.getImports(), containsImportOf(Generated.class));
	}

	@Test
	public void testGenerateMatcherFor_MatcherHasBeenCreated_GeneratedTypeIsAnnotatedWithGenerated() throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type);

		// Assertion
		final String source = readGeneratedSourceFileLines();
		final CompilationUnit javaFile = JavaParser.parse(source);

		final TypeDeclaration<?> generatedClass = javaFile.getType(0);
		final String annotationName = Generated.class.getSimpleName();
		final Optional<AnnotationExpr> generatedAnnotation = generatedClass.getAnnotationByName(annotationName);
		final List<StringLiteralExpr> childNodesByType = generatedAnnotation.get()
				.getChildNodesByType(StringLiteralExpr.class);
		final String generatedValue = childNodesByType.get(0).getValue();
		assertThat("Value of Generated Annotation", generatedValue,
				is(equalTo(JavaPoetHasPropertyMatcherClassGenerator.class.getName())));
	}

	private Matcher<Iterable<? super ImportDeclaration>> containsImportOf(final Class<Generated> importType) {
		return hasItem(hasProperty("name", hasToString(equalTo(importType.getName()))));
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
				hasItem(is(matcherConsumingMethodWithReturntypeAndName(generatedMatcherClass, "withSimpleProp"))));
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
				hasItem(is(matcherConsumingMethodWithReturntypeAndName(generatedMatcherClass, "withSimpleProp"))));
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
	public void testGenerateMatcherFor_GeneratedInstanceHasMatcherSetAndNotEqualValueIsGiven_ShouldNotMatch()
			throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);

		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		MethodUtils.invokeMethod(matcher, "withSimpleProp", "someValue");

		// Execution
		final boolean matches = matcher.matches(new SimplePojo("someOtherValue"));

		// Assertion
		assertThat("Matcher matches matching class", matches, is(false));
	}

	@Test
	public void testGenerateMatcherFor_GeneratedInstanceHasMatcherSetAndEqualValueIsGiven_ShouldMatch()
			throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);

		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		MethodUtils.invokeMethod(matcher, "withSimpleProp", "someValue");

		// Execution
		final boolean matches = matcher.matches(new SimplePojo("someValue"));

		// Assertion
		assertThat("Matcher matches matching class", matches, is(true));
	}

	@Test
	public void testGenerateMatcherFor_GeneratedInstanceHasBeenGenerated_NoConvenienceMethodShouldExist()
			throws Exception {

		// Preparation
		final Class<PojoWithMatcherProperty> type = PojoWithMatcherProperty.class;

		// Execution
		classUnderTest.generateMatcherFor(type);

		// Assertion
		final Matcher<PojoWithMatcherProperty> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);
		final List<Method> methods = getMethodsByName(matcher, "withSomeProperty");
		assertThat("Matcher matches matching class", methods, hasSize(1));
	}

	private List<Method> getMethodsByName(final Matcher<PojoWithMatcherProperty> matcher, final String methodName) {
		return Arrays.stream(matcher.getClass().getMethods()).filter(method -> methodName.equals(method.getName()))
				.collect(Collectors.toList());
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

	@Test
	public void testGenerateMatcherFor_StaticMethodWithBaseTypeNameIsCalled_ShouldReturnAnInstanceOfTheMatcher()
			throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type);

		final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(type);

		// Execution
		final Object result = MethodUtils.invokeStaticMethod(matcher.getClass(), "isSimplePojo");

		// Assertion
		assertThat(result.getClass().getSimpleName(), is(equalTo("SimplePojo" + MATCHER_POSTFIX)));
	}

	private Collection<Method> nonSyntheticMethodsOf(final Class<?> generatedMatcherClass) {
		return Arrays.stream(generatedMatcherClass.getDeclaredMethods()).filter(m -> !m.isSynthetic())
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private Matcher<Method> matcherConsumingMethodWithReturntypeAndName(final Class<?> generatedMatcherClass,
			final String propertyName) {
		return allOf(hasProperty("name", equalTo(propertyName)),
				hasProperty("parameterTypes", arrayContaining(Matcher.class)),
				hasProperty("returnType", is(generatedMatcherClass)));
	}

	private String readGeneratedSourceFileLines() throws IOException {
		return FileUtils.readFileToString(generatedSourceFileFor(SimplePojo.class), StandardCharsets.UTF_8);
	}

	private File generatedSourceFileFor(final Class<?> type) {
		return compiler.getGeneratedSourcePathFor(type).toFile();
	}
}
