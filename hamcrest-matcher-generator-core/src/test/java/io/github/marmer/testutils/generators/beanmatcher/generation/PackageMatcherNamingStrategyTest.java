package io.github.marmer.testutils.generators.beanmatcher.generation;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import sample.classes.ComplexSample;
import sample2.classes.SimplePojo;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PackageMatcherNamingStrategyTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @InjectMocks
    private PackageMatcherNamingStrategy underTest;

    @Test
    public void testTypeNameFor_TypeGiven_ShouldReturnNameOfTypeWithPostfix()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(SimplePojo.class);

        // Assertion
        assertThat(result.get(), is("SimplePojo"));
    }

    @Test
    public void testTypeNameFor_InnerTypeGiven_ShouldReturnNameOfTypeOnlyWithPostfix()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(ComplexSample.InnerClass.class);

        // Assertion
        assertThat(result.get(), is("InnerClass"));
    }

    @Test
    public void testTypeNameFor_NothingGiven_ShouldReturnNothing()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(null);

        // Assertion
        result.ifPresent(s -> fail("empty optional expected"));
    }

}