package io.github.marmer.testutils.samplepojos;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.FileResourceStore;
import org.apache.commons.lang3.reflect.MethodUtils;

import org.hamcrest.Matcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import org.mockito.InjectMocks;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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


public class HasPropertyMatcherGeneratorTest {
	private static final String JAVA_FILE_POSTFIX = ".java";
	private static final String SOURCE_ENCODING = "UTF-8";
	private static final String JAVA_VERSION = "1.7";
	private static final String MATCHER_POSTFIX = "Matcher";
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
	@InjectMocks
	private HasPropertyMatcherGenerator classUnderTest;
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();
	private Path srcOutputDir;
	private final JavaCompiler compiler = new JavaCompilerFactory().createCompiler("eclipse");
	private final JavaCompilerSettings compilerSettings = compiler.createDefaultSettings();
	private Path classOutputDir;

	@Before
	public void setUp() throws Exception {
		prepareSourceOutputDir();
		prepareOutputDir();
		getClassLoader();
		initCompilerSettings();
	}

	public void prepareOutputDir() throws Exception {
		this.classOutputDir = temp.newFolder("target").toPath();

	}

	private URLClassLoader getClassLoader() throws MalformedURLException {
		final URL url = classOutputDir.toUri().toURL();
		return new URLClassLoader(new URL[] {
		            url
		        });
	}

	public void prepareSourceOutputDir() throws Exception {
		this.srcOutputDir = temp.newFolder("src").toPath();
	}

	public void initCompilerSettings() {
		compilerSettings.setSourceEncoding(SOURCE_ENCODING);
		compilerSettings.setSourceVersion(JAVA_VERSION);
		compilerSettings.setTargetVersion(JAVA_VERSION);
	}

	@Test
	public void testGenerateMatcherFor_SimplePojoClassGiven_ShouldCreateJavaFile() throws Exception {

		// Preparation
		classUnderTest.generateMatcherFor(SimplePojo.class, srcOutputDir);

		// Assertion
		assertThat(generatedSourceFileFor(SimplePojo.class), is(anExistingFile()));
	}

	@Test
	public void testGenerateMatcherFor_FileHasBeanCreated_CreatedJavaFileShouldBeCompilableWithoutAnyIssues()
	    throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type, srcOutputDir);

		// Assertion
		final CompilationResult result = compileGeneratedSourceFileFor(type);
		assertThat(result, hasNoErrorsOrWarnings());
	}

	@Test
	public void testGenerateMatcherFor_FileHasBeanCreated_ShouldBeAbleToLoadAndInstanciateGeneratedClass()
	    throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type, srcOutputDir);

		// Assertion
		loadInstanceOfGeneratedClassFor(type);
	}

	@Test
	public void testGenerateMatcherFor_InstanceOfGeneratedMatcherHasBeenCreated_GeneratedInstanceCanBeUsedToMatchRelatedInstances()
	    throws Exception {

		// Preparation
		final Class<SimplePojo> type = SimplePojo.class;
		classUnderTest.generateMatcherFor(type, srcOutputDir);

		final Matcher<SimplePojo> matcher = loadInstanceOfGeneratedClassFor(type);

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
		classUnderTest.generateMatcherFor(type, srcOutputDir);

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
		classUnderTest.generateMatcherFor(type, srcOutputDir);
		compileGeneratedSourceFileFor(type);

		// Execution
		final Class<Matcher<SimplePojo>> generatedMatcherClass = loadGeneratedClassFor(type);

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
		classUnderTest.generateMatcherFor(type, srcOutputDir);
		compileGeneratedSourceFileFor(type);

		// Execution
		final Class<Matcher<SimplePojoChild>> generatedMatcherClass = loadGeneratedClassFor(type);

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
		classUnderTest.generateMatcherFor(type, srcOutputDir);
		final Matcher<SimplePojo> matcher = loadInstanceOfGeneratedClassFor(type);
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
		classUnderTest.generateMatcherFor(type, srcOutputDir);
		final Matcher<SimplePojo> matcher = loadInstanceOfGeneratedClassFor(type);
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
		classUnderTest.generateMatcherFor(type, srcOutputDir);
		final Matcher<SimplePojo> matcher = loadInstanceOfGeneratedClassFor(type);

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
		        hasProperty("name", equalTo(propertyName)), // TODO add "with" prefix
		        hasProperty("parameterTypes", arrayContaining(Matcher.class)),
		        hasProperty("returnType", is(generatedMatcherClass)));
	}

	private Matcher<Iterable<? extends String>> hasGeneratedAnnotationBeforeGeneratedClassDefinitionFor(
	    final Class<SimplePojo> type) {
		return containsInRelativeOrder(startsWith(
		            "@Generated(\"" + HasPropertyMatcherGenerator.class.getName() + "\")"),
		        containsString("class " + generatedMatcherClassNameFor(type)));
	}

	private List<String> readGeneratedSourceFileLines() throws IOException {
		return Files.readAllLines(generatedSourceFileFor(SimplePojo.class).toPath());
	}

	private <T> Matcher<T> loadInstanceOfGeneratedClassFor(final Class<SimplePojo> type) throws IOException, Exception,
	    InstantiationException, IllegalAccessException {
		compileGeneratedSourceFileFor(type);

		final Class<Matcher<T>> loadClass = loadGeneratedClassFor(type);

		return (Matcher<T>) loadClass.newInstance();
	}

	private <T> Class<Matcher<T>> loadGeneratedClassFor(final Class<?> type) throws Exception {
		return (Class<Matcher<T>>) getClassLoader().loadClass(generatedFullQualifiedClassNameFor(type));
	}

	private String generatedFullQualifiedClassNameFor(final Class<?> type) {
		return getPackageNameOf(type) + "." + generatedMatcherClassNameFor(type);
	}

	private CompilationResult compileGeneratedSourceFileFor(final Class<?> type) throws IOException {
		final String[] pResourcePaths = {
		    getGeneratedRelativePathOfUnixString(type)
		};
		final FileResourceReader sourceFolderResource = new FileResourceReader(srcOutputDir.toFile());
		final FileResourceStore classFolderResource = new FileResourceStore(classOutputDir.toFile());

		return compiler.compile(pResourcePaths, sourceFolderResource, classFolderResource, getClass().getClassLoader(),
		        compilerSettings);
	}

	private String getGeneratedRelativePathOfUnixString(final Class<?> type) {
		return getGeneratedRelativePathOf(type).toString().replaceAll("\\\\", "/");
	}

	private File generatedSourceFileFor(final Class<?> type) {
		return generatedSourcePathFor(type).toFile();
	}

	private Path generatedSourcePathFor(final Class<?> type) {
		return srcOutputDir.resolve(getGeneratedRelativePathOf(type));
	}

	private Path getGeneratedRelativePathOf(final Class<?> type) {
		return getPackagePath(type).resolve(generatedMatcherJavaFileNameFor(type));
	}

	private String generatedMatcherJavaFileNameFor(final Class<?> type) {
		return generatedMatcherClassNameFor(type) + JAVA_FILE_POSTFIX;
	}

	private String generatedMatcherClassNameFor(final Class<?> type) {
		return type.getSimpleName() + MATCHER_POSTFIX;
	}

	private Path getPackagePath(final Class<?> type) {
		return Paths.get(getPackageNameOf(type).replaceAll("\\.", "/"));
	}

	private String getPackageNameOf(final Class<?> type) {
		return type.getPackage().getName();
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
