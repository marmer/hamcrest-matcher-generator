package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.plugin.testing.MojoRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class GenerateMatchersMojoTest {
	@Rule
	public MojoRule rule = new MojoRule();

	@Test
	public void testSomething() throws Exception {
		rule.getPluginDescriptorPath();
		final GenerateMatchersMojo myMojo = (GenerateMatchersMojo) rule.lookupMojo("matchers",
				"src/test/resources/unit/testproject/pom.xml");
		assertNotNull(myMojo);
		myMojo.execute();
	}
}
