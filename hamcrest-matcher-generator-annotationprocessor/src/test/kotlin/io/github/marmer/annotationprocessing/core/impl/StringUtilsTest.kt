package io.github.marmer.annotationprocessing.core.impl

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks

internal class StringUtilsTest {
    @InjectMocks
    private val underTest: StringUtils? = null

    @Test
    @DisplayName("Given null should be returned as null")
    fun testCapitalize_GivenNullShouldBeReturnedAsNull() {
        // Preparation

        // Execution
        val result = StringUtils.capitalize(null)

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`(Matchers.nullValue()))
    }

    @Test
    @DisplayName("An empty string should be returned as is")
    fun testCapitalize_AnEmptyStringShouldBeReturnedAsIs() {
        // Preparation

        // Execution
        val result = StringUtils.capitalize("")

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`(""))
    }

    @Test
    @DisplayName("Given single char should be uppercase")
    fun testCapitalize_GivenSingleCharShouldBeUppercase() {
        // Preparation

        // Execution
        val result = StringUtils.capitalize("a")

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`("A"))
    }

    @Test
    @DisplayName("First char of a string with multiple chars should get capitalized")
    fun testCapitalize_FirstCharOfAStringWithMultipleCharsShouldGetCapitalized() {
        // Preparation

        // Execution
        val result = StringUtils.capitalize("ab")

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`("Ab"))
    }

    @Test
    @DisplayName("Given null should be returned as null")
    fun testUncapitalize_GivenNullShouldBeReturnedAsNull() {
        // Preparation

        // Execution
        val result = StringUtils.uncapitalize(null)

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`(Matchers.nullValue()))
    }

    @Test
    @DisplayName("An empty string should be returned as is")
    fun testUncapitalize_AnEmptyStringShouldBeReturnedAsIs() {
        // Preparation

        // Execution
        val result = StringUtils.uncapitalize("")

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`(""))
    }

    @Test
    @DisplayName("Given single char should be lowercase")
    fun testUncapitalize_GivenSingleCharShouldBeLowercase() {
        // Preparation

        // Execution
        val result = StringUtils.uncapitalize("A")

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`("a"))
    }

    @Test
    @DisplayName("First char of a string with multiple chars should get uncapitalized")
    fun testUncapitalize_FirstCharOfAStringWithMultipleCharsShouldGetUncapitalized() {
        // Preparation

        // Execution
        val result = StringUtils.uncapitalize("AB")

        // Assertion
        MatcherAssert.assertThat(result, Matchers.`is`("aB"))
    }
}
