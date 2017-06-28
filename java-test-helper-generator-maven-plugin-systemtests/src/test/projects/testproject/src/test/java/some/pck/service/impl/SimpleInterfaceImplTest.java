package some.pck.service.impl;

import org.junit.Test;

import some.pck.model.SimpleModel;
import some.pck.model.SimpleModelMatcher;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.equalTo;

import static org.junit.Assert.assertThat;


public class SimpleInterfaceImplTest {
	@Test
	public void testSomeTestWhichDoingSomething() throws Exception {
		assertThat(new SimpleModel("someValue"), is(new SimpleModelMatcher().withSomeProperty(equalTo("someValue"))));
	}

}
