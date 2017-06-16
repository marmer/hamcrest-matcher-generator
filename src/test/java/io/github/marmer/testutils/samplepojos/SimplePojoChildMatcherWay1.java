package io.github.marmer.testutils.samplepojos;

import io.github.marmer.testutils.BeanPropertyMatcher;
import io.github.marmer.testutils.samplepojos.HasPropertyMatcherGeneratorTest.SimplePojoChild;

import org.hamcrest.Matcher;

import javax.annotation.Generated;


/**
 * TODO DELETE ME!
 *
 * @author  marmer
 * @since   16.06.2017
 */
@Generated("io.github.marmer.testutils.samplepojos.HasPropertyMatcherGenerator")
public class SimplePojoChildMatcherWay1 extends BeanPropertyMatcher<SimplePojoChild> {
	public SimplePojoChildMatcherWay1() {
		super(HasPropertyMatcherGeneratorTest.SimplePojoChild.class);
	}

	public SimplePojoChildMatcherWay1 withSimpleProp(final Matcher<?> matcher) {
		with("simpleProp", matcher);
		return this;
	}
}
