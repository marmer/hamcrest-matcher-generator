package org.java.test.helper.generator.maven.plugin.testutils;

import org.apache.commons.io.FileUtils;

import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import org.codehaus.plexus.util.cli.CommandLineException;

import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;


public class TestProjectResource extends TestResources {
	private File baseDir;

	public TestProjectResource(final String projectName) {
		super();
		try {
			getBasedir(projectName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public File getBaseDir() {
		return baseDir;
	}

	@Override
	public File getBasedir(final String projectName) throws IOException {
		FileUtils.deleteQuietly(baseDir);
		baseDir = super.getBasedir("projectName");
		return baseDir;
	}

	@Override
	protected void starting(final Description description) {
		super.starting(description);
	}

	@Override
	protected void finished(final Description description) {
		super.finished(description);
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

	public int executeGoals(
		final String... goals) throws MavenInvocationException, CommandLineException {
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
