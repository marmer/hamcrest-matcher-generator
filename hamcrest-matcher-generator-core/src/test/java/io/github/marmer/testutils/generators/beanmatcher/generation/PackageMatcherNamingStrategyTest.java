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

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.junit.Assert.assertThat;

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
        assertThat(result, isPresentAndIs("SimplePojo"));
    }

    @Test
    public void testTypeNameFor_InnerTypeGiven_ShouldReturnNameOfTypeOnlyWithPostfix()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.typeNameFor(ComplexSample.InnerClass.class);

        // Assertion
        assertThat(result, isPresentAndIs("InnerClass"));
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

    @Test
    public void testPackageFor_TypeGiven_ShouldReturnPackageOfType()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.packageFor(SimplePojo.class);

        // Assertion
        assertThat(result, isPresentAndIs("sample2.classes"));
    }

    @Test
    public void testPackageFor_InnerClassTypeGiven_ShouldReturnPackageOfSuperTypeWithEndpackageNamedLikesEnclosingClass()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.packageFor(ComplexSample.InnerClass.class);

        // Assertion
        assertThat(result, isPresentAndIs("sample.classes.complexsample"));
    }

    @Test
    public void testPackageFor_InnerInnerClassTypeGiven_ShouldReturnPackageOfSuperTypesWithEndpackageNamedLikesEnclosingClass()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.packageFor(sample2.classes.ContainerClass.InnerClass.InnerInnerClass.class);

        // Assertion
        assertThat(result, isPresentAndIs("sample2.classes.containerclass.innerclass"));
    }

    @Test
    public void testPackageFor_NothingGiven_ShouldReturnNothing()
            throws Exception {
        // Preparation

        // Execution
        final Optional<String> result = underTest.packageFor(null);

        // Assertion
        assertThat(result, isEmpty());
    }
}