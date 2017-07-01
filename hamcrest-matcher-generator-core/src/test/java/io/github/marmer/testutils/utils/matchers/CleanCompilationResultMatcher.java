package io.github.marmer.testutils.utils.matchers;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.problems.CompilationProblem;
import org.apache.commons.lang3.ArrayUtils;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;


public class CleanCompilationResultMatcher extends TypeSafeMatcher<CompilationResult> {
	@Override
	public void describeTo(final Description description) {
		description.appendText("CompilationResult without any errors and warnings");
	}

	@Override
	protected boolean matchesSafely(final CompilationResult item) {
		return ArrayUtils.isEmpty(item.getErrors()) && ArrayUtils.isEmpty(item.getWarnings());
	}

	@Override
	protected void describeMismatchSafely(final CompilationResult item,
	    final Description mismatchDescription) {
		mismatchDescription.appendText("contains");

		final CompilationProblem[] errors = item.getErrors();

		if (!ArrayUtils.isEmpty(errors)) {

			for (final CompilationProblem error : errors) {
				mismatchDescription.appendText("\n        Error: ").appendValue(error);
			}
		}

		final CompilationProblem[] warnings = item.getWarnings();

		if (!ArrayUtils.isEmpty(warnings)) {

			for (final CompilationProblem warning : warnings) {
				mismatchDescription.appendText("\n        Error: ").appendValue(warning);
			}
		}
	}

	public static CleanCompilationResultMatcher hasNoErrorsOrWarnings() {
		return new CleanCompilationResultMatcher();
	}
}
