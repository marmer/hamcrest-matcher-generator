package some.pck.service.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static some.pck.model.SimpleModelMatcher.isSimpleModel;

import org.junit.Test;

import some.pck.model.SimpleModel;
import some.pck.model.SimpleModelMatcher;


public class SimpleInterfaceImplTest {
	@Test
	public void testMatchingWithConstructor() throws Exception {
		assertThat(new SimpleModel("someValue"), is(new SimpleModelMatcher().withSomeProperty(equalTo("someValue"))));
	}
	
	@Test
	public void testMatchingWithStaticMethod() throws Exception {
		assertThat(new SimpleModel("someValue"), isSimpleModel().withSomeProperty(equalTo("someValue")));
	}

}
