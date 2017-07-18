package io.github.marmer.testutils.generators.beanmatcher.processing;

import be.joengenduvel.java.verifiers.ToStringVerifier;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;


public class BeanPropertyTest {

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(BeanProperty.class).verify();
	}

	@Test
	public void shouldImplementToString() {
		final BeanProperty sut = new BeanProperty("name", BeanPropertyTest.class);
		ToStringVerifier.forClass(BeanProperty.class).ignore("$jacocoData").containsAllPrivateFields(sut);
	}

}
