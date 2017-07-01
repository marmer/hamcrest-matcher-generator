package org.java.test.helper.generator.maven.plugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.ReflectionUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MatchersMojoTest {
	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@InjectMocks
	private MatchersMojo classUnderTest;

	@Mock
	private MavenProject mavenProject;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Spy
	private File outputDir = new File("outputDir");

	@Spy
	private PathToUrlDelegate pathToUrlDelegate = new PathToUrlDelegate();

	private String[] matcherSources = new String[] {
		"some.package"
	};

	@Mock
	private ClassLoaderFactory classloaderFactory;

	@Mock
	private MatcherGeneratorFactory matcherGeneratorFactory;

	private final List<String> classpathElements = Collections.singletonList("anyClasspathElement");

	private final ClassLoader classLoader = mock(ClassLoader.class);

	private final MatcherGenerator matcherGenerator = mock(MatcherGenerator.class);

	@Before
	public void setUp() throws Exception {
		ReflectionUtils.setVariableValueInObject(classUnderTest, "matcherSources", matcherSources);
	}

	@Test
	public void testExecute_MavenClassloaderIsNotAbleToResolveRequiredDependencies_ShouldBreakTheBuild()
		throws Exception {

		// Preparation
		final DependencyResolutionRequiredException dependencyResolutionRequiredException = mock(
				DependencyResolutionRequiredException.class);
		when(mavenProject.getTestClasspathElements()).thenThrow(dependencyResolutionRequiredException);

		// Expectation
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(equalTo("Cannot access Dependencies")));
		exception.expectCause(is(dependencyResolutionRequiredException));

		// Execution
		classUnderTest.execute();
	}

	@Test
	public void testExecute_GenerationOfClasspathWorks_OutputDirPathShouldBeUsedAsTestSource() throws Exception {
		readyForGeneration();

		// Execution
		classUnderTest.execute();

		// Assertion
		verify(mavenProject).addTestCompileSourceRoot(outputDir.toString());
	}

	@Test
	public void testExecute_NoPackagesOrQualifiedClassnamesGivenForExecution_ShouldStopTheBuild() throws Exception {

		// Preparation
		ReflectionUtils.setVariableValueInObject(classUnderTest, "matcherSources", new String[0]);

		// Assertion
		exception.expect(MojoFailureException.class);
		exception.expectMessage(
			"Missing MatcherSources. You should at least add one Package or qualified class name in matcherSources");

		// Execution
		classUnderTest.execute();
	}

	@Test
	public void testExecute_ClassloaderCanBeCreatedByClasspathElements_GenerationShouldStart() throws Exception {

		// Preparation
		readyForGeneration();

		// Execution
		classUnderTest.execute();

		// Assertion
		verify(matcherGenerator).generateHelperForClassesAllIn(matcherSources);
	}

	@Test
	public void testExecute_AnErrorHappensOnGeneration_BuildShouldStopWithAnAppropriatErrorMessage() throws Exception {

		// Preparation
		readyForGeneration();
		final IOException ioException = new IOException("some error message");
		doThrow(ioException).when(matcherGenerator).generateHelperForClassesAllIn(matcherSources);

		// Assertion
		exception.expect(MojoFailureException.class);
		exception.expectCause(is(sameInstance(ioException)));
		exception.expectMessage(is(equalTo("Error on matcher generation")));

		// Execution
		classUnderTest.execute();

	}

	private void readyForGeneration() throws Exception {
		classLoaderIsReady();
		when(matcherGeneratorFactory.createBy(classLoader, outputDir.toPath())).thenReturn(matcherGenerator);
	}

	private void classLoaderIsReady() throws Exception {
		classpathElementsExist();
		when(classloaderFactory.creatClassloader(classpathElements)).thenReturn(classLoader);
	}

	private void classpathElementsExist() throws Exception {
		when(mavenProject.getTestClasspathElements()).thenReturn(classpathElements);
	}
}