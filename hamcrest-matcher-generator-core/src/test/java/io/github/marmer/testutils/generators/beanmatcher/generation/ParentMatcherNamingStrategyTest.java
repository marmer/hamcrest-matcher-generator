package io.github.marmer.testutils.generators.beanmatcher.generation;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import sample.classes.ComplexSample;
import sample2.classes.SimplePojo;

import java.util.Optional;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ParentMatcherNamingStrategyTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    @InjectMocks
    private ParentMatcherNamingStrategy underTest;

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

    @Test
    public void testTypeNameFor_TypeGiven_ShouldReturnPackageOfType()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(SimplePojo.class);

        // Assertion
        assertThat(result, isPresentAndIs("SimplePojoMatcher"));
    }

    @Test
    public void testTypeNameFor_InnerClassTypeGiven_ShouldReturnPackageOfSuperTypeWithEndpackageNamedLikesEnclosingClass()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(ComplexSample.InnerClass.class);

        // Assertion
        assertThat(result, isPresentAndIs("ComplexSample$InnerClassMatcher"));
    }

    @Test
    public void testTypeNameFor_InnerInnerClassTypeGiven_ShouldReturnPackageOfSuperTypesWithEndpackageNamedLikesEnclosingClass()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(sample2.classes.ContainerClass.InnerClass.InnerInnerClass.class);

        // Assertion
        assertThat(result, isPresentAndIs("ContainerClass$InnerClass$InnerInnerClassMatcher"));
    }

    @Test
    public void testTypeNameFor_NothingGiven_ShouldReturnNothing()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(null);

        // Assertion
        assertThat(result, isEmpty());
    }
}