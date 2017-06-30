package org.java.test.helper.generator.maven.plugin.testutils;

import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import org.codehaus.plexus.util.cli.CommandLineException;

import org.junit.rules.TestWatcher;

import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.function.Consumer;


public class TestProjectResource extends TestWatcher {

	private File baseDir;

	private Consumer<Description> startingMethod;
	private Consumer<Description> finishedMethod;

	public TestProjectResource(final String projectName) {
		new TestResources() {

			{
				startingMethod = this::starting;
				finishedMethod = this::finished;
			}

			@Override
			protected void starting(final Description description) {
				try {
					super.starting(description);
					baseDir = getBasedir(projectName);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@Override
	protected void starting(final Description description) {
		startingMethod.accept(description);
	}

	@Override
	protected void finished(final Description description) {
		finishedMethod.accept(description);
	}

	public File getBaseDir() {
		return baseDir;
	}

	public File generatedTestSourcesDir(final String path) {
		return new File(generatedTestSourcesDir(), path);
	}

	public File srcMainJava(final String path) {
		return new File(getSrcMainJavaDir(), path);
	}

	public File getSrcMainJavaDir() {
		return new File(baseDir, "src/main/java");
	}

	public File generatedTestSourcesDir() {
		return new File(targetDir(), "generated-test-sources");
	}

	public File targetDir() {
		return new File(baseDir, "target");
	}

	public File pomFile() {
		return new File(baseDir, "pom.xml");
	}

	public int executeGoals(final String... goals) throws MavenInvocationException, CommandLineException {
		final InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(pomFile());
		request.setGoals(Arrays.asList(goals));

		final Invoker invoker = new DefaultInvoker();
		prepareExecutableFor(invoker);

		final InvocationResult result = invoker.execute(request);

		if (result.getExecutionException() != null) {
			throw result.getExecutionException();
		}

		return result.getExitCode();
	}

	private void prepareExecutableFor(final Invoker invoker) {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			final String getenv = System.getenv("M2_HOME");
			File mavenExecutable = new File(getenv, "bin/mvn.cmd");

			if (!mavenExecutable.exists()) {
				mavenExecutable = new File(getenv, "bin/mvn.bat");
			}

			invoker.setMavenExecutable(mavenExecutable);
		}
	}

}
