package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory.MatcherGeneratorConfiguration;
import io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class MatchersMojoTest {
	private final List<String> classpathElements = singletonList("anyClasspathElement");
	private final ClassLoader classLoader = mock(ClassLoader.class);
	private final MatcherGenerator matcherGenerator = mock(MatcherGenerator.class);
	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
	@Rule
	public ExpectedException exception = ExpectedException.none();
	@InjectMocks
	private MatchersMojo classUnderTest;
	@Mock
	private MavenProject mavenProject;
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
	@Mock
	private ProjectDependenciesResolver projectDependenciesResolver;

	@Mock
	private MavenSession mavenSession;

	@Mock
	private DependencyValidatorFactory dependencyValidatorFactory;

	@Mock
	private DependencyValidator dependencyValidator;

	@Mock
	private Log log;
	@Before
	public void setUp() throws Exception {
		ReflectionUtils.setVariableValueInObject(classUnderTest, "matcherSources", matcherSources);
		ReflectionUtils.setVariableValueInObject(classUnderTest,
			"allowMissingHamcrestDependency",
			false);

	}

	@Test
	public void testExecute_MissingRequiredDependenciesAreNotAllowedAndDependenciesAreMissing_BuildShouldBreak()
		throws Exception {

		// Vorbereitung
		ReflectionUtils.setVariableValueInObject(classUnderTest,
			"allowMissingHamcrestDependency",
			false);

		final MojoFailureException buildBrakingException = new MojoFailureException("any needed dependency is missing");
		doReturn(dependencyValidator).when(dependencyValidatorFactory).createBy(mavenProject,
			projectDependenciesResolver, mavenSession);
		doThrow(buildBrakingException).when(dependencyValidator).validateProjectHasNeededDependencies();

		// Prüfung
		exception.expect(is(buildBrakingException));

		// Ausführung
		classUnderTest.execute();
	}

	@Test
	public void testExecute_MissingRequiredDependenciesAreAllowedAndDependenciesAreMissingAndAllOtherRequirementsForGenerationAreGood_GenerationShouldStart()
		throws Exception {

		// Preparation
		ReflectionUtils.setVariableValueInObject(classUnderTest,
			"allowMissingHamcrestDependency",
			true);
		readyForGeneration();

		// Execution
		classUnderTest.execute();

		// Assertion
		verify(matcherGenerator).generateHelperForClassesAllIn(matcherSources);
	}

	@Test
	public void testExecute_MavenClassloaderIsNotAbleToResolveRequiredDependencies_ShouldBreakTheBuild()
		throws Exception {

		// Preparation
		projectDependenceisAreValid();

		final DependencyResolutionRequiredException dependencyResolutionRequiredException = mock(
				DependencyResolutionRequiredException.class);
		when(mavenProject.getTestClasspathElements()).thenThrow(
			dependencyResolutionRequiredException);

		// Expectation
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(equalTo("Cannot access Dependencies")));
		exception.expectCause(is(dependencyResolutionRequiredException));

		// Execution
		classUnderTest.execute();
	}

	private void projectDependenceisAreValid() throws MojoFailureException {
		doReturn(dependencyValidator).when(dependencyValidatorFactory).createBy(any(MavenProject.class),
			any(ProjectDependenciesResolver.class), any(MavenSession.class));
		doNothing().when(dependencyValidator).validateProjectHasNeededDependencies();
	}

	@Test
	public void testExecute_GenerationOfClasspathWorks_OutputDirPathShouldBeUsedAsTestSource() throws Exception {
		projectDependenceisAreValid();
		readyForGeneration();

		// Execution
		classUnderTest.execute();

		// Assertion
		verify(mavenProject).addTestCompileSourceRoot(outputDir.toString());
	}

	@Test
	public void testExecute_NoPackagesOrQualifiedClassnamesGivenForExecution_ShouldStopTheBuild() throws Exception {

		// Preparation
		projectDependenceisAreValid();
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
		projectDependenceisAreValid();
		readyForGeneration();

		// Execution
		classUnderTest.execute();

		// Assertion
		verify(matcherGenerator).generateHelperForClassesAllIn(matcherSources);
	}

	@Test
	public void testExecute_MatchersHaveBeenGenerated_GeneratedMatchersAreLogged() throws Exception {

		// Preparation
		projectDependenceisAreValid();
		readyForGeneration();

		final Path someHelper = Paths.get("some", "path");
		when(matcherGenerator.generateHelperForClassesAllIn(matcherSources)).thenReturn(singletonList(
				someHelper));

		// Execution
		classUnderTest.execute();

		// Assertion
		verify(log).info("Generated: " + someHelper);
	}

	@Test
	public void testExecute_AnErrorHappensOnGeneration_BuildShouldStopWithAnAppropriatErrorMessage() throws Exception {

		// Preparation
		projectDependenceisAreValid();
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

		final boolean ignoreClassesWithoutProperties = true;
		ReflectionUtils.setVariableValueInObject(classUnderTest,
				"ignoreClassesWithoutProperties",
				ignoreClassesWithoutProperties);
		ReflectionUtils.setVariableValueInObject(classUnderTest,
				"namingStrategy",
				MatcherNamingStrategy.Name.PLAIN);

		final MatcherGeneratorConfiguration matcherGeneratorConfiguration = MatcherGeneratorConfiguration.builder()
				.classLoader(classLoader)
				.outputPath(outputDir.toPath())
				.ignoreClassesWithoutProperties(ignoreClassesWithoutProperties)
				.namingStrategy(MatcherNamingStrategy.Name.PLAIN)
				.log(new MojoLog(log))
				.build();
		when(matcherGeneratorFactory.createBy(matcherGeneratorConfiguration)).thenReturn(
			matcherGenerator);
	}

	private void classLoaderIsReady() throws Exception {
		classpathElementsExist();
		when(classloaderFactory.creatClassloader(classpathElements)).thenReturn(classLoader);
	}

	private void classpathElementsExist() throws Exception {
		when(mavenProject.getTestClasspathElements()).thenReturn(classpathElements);
	}
}
