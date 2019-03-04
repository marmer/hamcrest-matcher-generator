package io.github.marmer.annotationprocessing.core.impl;

import lombok.var;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

class StringUtilsTest {
    @InjectMocks
    private StringUtils underTest;

    @Test
    @DisplayName("Given null should be returned as null")
    void testCapitalize_GivenNullShouldBeReturnedAsNull()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.capitalize(null);

        // Assertion
        Assert.assertThat(result, is(nullValue()));
    }

    @Test
    @DisplayName("An empty string should be returned as is")
    void testCapitalize_AnEmptyStringShouldBeReturnedAsIs()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.capitalize("");

        // Assertion
        assertThat(result, is(""));
    }

    @Test
    @DisplayName("Given single char should be uppercase")
    void testCapitalize_GivenSingleCharShouldBeUppercase()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.capitalize("a");

        // Assertion
        assertThat(result, is("A"));
    }

    @Test
    @DisplayName("First char of a string with multiple chars should get capitalized")
    void testCapitalize_FirstCharOfAStringWithMultipleCharsShouldGetCapitalized()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.capitalize("ab");

        // Assertion
        assertThat(result, is("Ab"));
    }

    @Test
    @DisplayName("Given null should be returned as null")
    void testUncapitalize_GivenNullShouldBeReturnedAsNull()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.uncapitalize(null);

        // Assertion
        Assert.assertThat(result, is(nullValue()));
    }

    @Test
    @DisplayName("An empty string should be returned as is")
    void testUncapitalize_AnEmptyStringShouldBeReturnedAsIs()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.uncapitalize("");

        // Assertion
        assertThat(result, is(""));
    }

    @Test
    @DisplayName("Given single char should be lowercase")
    void testUncapitalize_GivenSingleCharShouldBeLowercase()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.uncapitalize("A");

        // Assertion
        assertThat(result, is("a"));
    }

    @Test
    @DisplayName("First char of a string with multiple chars should get uncapitalized")
    void testUncapitalize_FirstCharOfAStringWithMultipleCharsShouldGetUncapitalized()
            throws Exception {
        // Preparation

        // Execution
        final var result = underTest.uncapitalize("AB");

        // Assertion
        assertThat(result, is("aB"));
    }
}