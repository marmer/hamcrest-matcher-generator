package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class MyMojoTest {
	@Rule
	public MojoRule rule = new MojoRule() {
		@Override
		protected void before() throws Throwable {
			super.before();
		}

		@Override
		protected void after() {
			super.after();
		}
	};

	@Test
	public void testSomething() throws Exception {
		final MyMojo myMojo = (MyMojo) rule.lookupMojo("touch", "src/test/resources/unit/testproject/pom.xml");
		assertNotNull(myMojo);
		myMojo.execute();

	}

	/** Do not need the MojoRule. */
	@WithoutMojo
	@Test
	public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() { }

}
