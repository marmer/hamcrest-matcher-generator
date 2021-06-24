package foo.bar;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InheritanceAndGenericLearningTest {

    @Test
    @DisplayName("A generics with inheritance learning test")
    @SneakyThrows
    void test_AGenericsWithInheritanceLearningTest() {
        final Bottom element = new Bottom() {
        };
        final Container<Top> topContainer = new Container<>(element);
        final Container<Mid> midContainer = new Container<>(element);
        final Container<Bottom> bottomContainer = new Container<>(element);

        MatcherAssert.assertThat(topContainer, getContainerSuperMatcher());
        MatcherAssert.assertThat(topContainer, (Matcher) getContainerExtendsMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(midContainer, getContainerSuperMatcher());
        MatcherAssert.assertThat(midContainer, getContainerExtendsMatcher());
        MatcherAssert.assertThat(bottomContainer, (Matcher) getContainerSuperMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(bottomContainer, getContainerExtendsMatcher());

        final List<Top> topList = List.of(element);
        final List<Mid> midList = List.of(element);
        final List<Bottom> bottomList = List.of(element);

        MatcherAssert.assertThat(topList, getListSuperMatcher());
        MatcherAssert.assertThat(topList, (Matcher) getListExtendsMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(midList, getListSuperMatcher());
        MatcherAssert.assertThat(midList, getListExtendsMatcher());
        MatcherAssert.assertThat(bottomList, (Matcher) getListSuperMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(bottomList, getListExtendsMatcher());

        MatcherAssert.assertThat(topList, getListSuperSuperMatcher());
        MatcherAssert.assertThat(topList, (Matcher) getListSuperExtendsMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(topList, (Matcher) getListExtendsSuperMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(topList, (Matcher) getListExtendsExtendsMatcher()); // cast trick for compiler

        MatcherAssert.assertThat(midList, getListSuperSuperMatcher());
        MatcherAssert.assertThat(midList, getListSuperExtendsMatcher());
        MatcherAssert.assertThat(midList, (Matcher) getListExtendsSuperMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(midList, (Matcher) getListExtendsExtendsMatcher()); // cast trick for compiler

        MatcherAssert.assertThat(bottomList, (Matcher) getListSuperSuperMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(bottomList, getListSuperExtendsMatcher());
        MatcherAssert.assertThat(bottomList, (Matcher) getListExtendsSuperMatcher()); // cast trick for compiler
        MatcherAssert.assertThat(bottomList, (Matcher) getListExtendsExtendsMatcher()); // cast trick for compiler
    }

    @NotNull
    private Matcher<Container<? super Mid>> getContainerSuperMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final Container<? super Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    @NotNull
    private Matcher<Container<? extends Mid>> getContainerExtendsMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final Container<? extends Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    @NotNull
    private Matcher<List<? super Mid>> getListSuperMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final List<? super Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    @NotNull
    private Matcher<List<? extends Mid>> getListExtendsMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final List<? extends Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    @NotNull
    private <T> Matcher<? extends List<? extends Mid>> getListExtendsExtendsMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final List<? extends Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    @NotNull
    private <T> Matcher<? extends List<? super Mid>> getListExtendsSuperMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final List<? super Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    @NotNull
    private <T> Matcher<? super List<? extends Mid>> getListSuperExtendsMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final List<? extends Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    @NotNull
    private <T> Matcher<? super List<? super Mid>> getListSuperSuperMatcher() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final List<? super Mid> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }

    public interface Top {

    }

    @Data
    @AllArgsConstructor
    public static class Container<T> {

        private T element;
    }

    public interface Mid extends Top {

    }

    public interface Bottom extends Mid {

    }

}
