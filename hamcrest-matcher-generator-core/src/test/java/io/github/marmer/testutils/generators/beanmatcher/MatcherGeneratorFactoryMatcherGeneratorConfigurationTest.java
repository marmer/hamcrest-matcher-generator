package io.github.marmer.testutils.generators.beanmatcher;

import be.joengenduvel.java.verifiers.ToStringVerifier;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory.MatcherGeneratorConfiguration;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Paths;


public class MatcherGeneratorFactoryMatcherGeneratorConfigurationTest {
	@Test
	public void equalsContract() throws Exception{
		EqualsVerifier.forClass(MatcherGeneratorConfiguration.class).withPrefabValues(ClassLoader.class,
			new URLClassLoader(new URL[] {
					new URL("file://1")
				}), new URLClassLoader(new URL[] {
					new URL("file://2")
				})).verify();
	}

	@Test
	public void shouldImplementToString() {
		final MatcherGeneratorConfiguration sut = MatcherGeneratorConfiguration.builder().classLoader(getClass()
				.getClassLoader()).outputPath(Paths.get("any")).build();
		ToStringVerifier.forClass(MatcherGeneratorConfiguration.class).ignore("$jacocoData").containsAllPrivateFields(
			sut);
	}
}
