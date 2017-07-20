package io.github.marmer.testutils.generators.beanmatcher;

import static org.hamcrest.Matchers.containsInAnyOrder;

import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.InjectMocks;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import sample.classes.SimpleSampleClass;

import java.lang.annotation.Annotation;

import java.util.Arrays;
import java.util.List;


public class JavaInternalIllegalClassFilterTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @InjectMocks
    private JavaInternalIllegalClassFilter classUnderTest;

    @SuppressWarnings("unchecked")
    @Test
    public void testFilter_NonIllegalAndJavaLangClassGiven_ShouldOnlyReturnNonIllegals()
        throws Exception {
        // Preparation
        final Class<SimpleSampleClass> anyLegalClass = SimpleSampleClass.class;
        final Class<NonIllegalException> anyJavaLangClassSubclass = NonIllegalException.class;
        final Class<NonIllegalRuntimeException> anyJavaLangSubclassSubclass =
            NonIllegalRuntimeException.class;
        final Class<NonIllegalAnnotation> anyJavaLangSubPackageSubclass =
            NonIllegalAnnotation.class;

        final Class<Exception> javaLangClass = Exception.class;
        final Class<RuntimeException> javaLangSubclass = RuntimeException.class;
        final Class<Annotation> javaLangSubpackageClass = Annotation.class;

        // Execution
        final List<Class<?>> nonIllegals = classUnderTest.filter(
                Arrays.asList(anyLegalClass,
                    anyJavaLangClassSubclass,
                    anyJavaLangSubclassSubclass,
                    anyJavaLangSubPackageSubclass,
                    javaLangClass,
                    javaLangSubclass,
                    javaLangSubpackageClass));

        // Expectation
        assertThat(nonIllegals,
            containsInAnyOrder(anyLegalClass,
                anyJavaLangClassSubclass,
                anyJavaLangSubclassSubclass,
                anyJavaLangSubPackageSubclass));
    }

    public static interface NonIllegalAnnotation extends Annotation {
    }

    public static class NonIllegalException extends Exception {
        /** Use serialVersionUID for interoperability. */
        private static final long serialVersionUID = 1L;
    }

    public static class NonIllegalRuntimeException extends RuntimeException {
        /** Use serialVersionUID for interoperability. */
        private static final long serialVersionUID = 1L;
    }
}
