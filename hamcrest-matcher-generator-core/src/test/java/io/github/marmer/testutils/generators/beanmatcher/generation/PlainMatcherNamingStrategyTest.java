package io.github.marmer.testutils.generators.beanmatcher.generation;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PlainMatcherNamingStrategyTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    @InjectMocks
    private PlainMatcherNamingStrategy underTest;

    @Test
    public void testPackageFor_TypeGiven_ShouldReturnThePackageOfThePassedClass()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.packageFor(sample.classes.subpackage2.ClassInSubPackage.class);

        // Assertion
        assertThat(result.get(), is("sample.classes.subpackage2"));
    }

    @Test
    public void testPackageFor_NullGiven_ShouldReturnNothing()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.packageFor(null);

        // Assertion
        result.ifPresent(Assert::fail);
    }

}