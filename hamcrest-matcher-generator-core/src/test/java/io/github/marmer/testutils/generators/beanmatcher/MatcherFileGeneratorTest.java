package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.IllegalClassFilter;
import io.github.marmer.testutils.generators.beanmatcher.processing.PotentialPojoClassFinder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


public class MatcherFileGeneratorTest {
    private final String packageName = "packageName";
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    @InjectMocks
    private MatcherFileGenerator classUnderTest;
    @Mock
    private PotentialPojoClassFinder potentialPojoClassFinder;
    @Mock
    private HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator;
    @Mock
    private Path outputDir;

    @Mock
    private IllegalClassFilter illegalClassFilter;

    @Test
    public void testGenerateHelperForClassesAllIn_PackageGiven_ClassesOfPackageShouldBeUsedToGenerateMatchersPerClassFound()
        throws Exception {
        // Preparation
        final List<Class<?>> baseClassList = asList(SamplePojo1.class,
                SamplePojo2.class);
        doReturn(baseClassList).when(potentialPojoClassFinder).findClasses(packageName);
        doReturn(baseClassList).when(illegalClassFilter).filter(baseClassList);

        // Execution
        classUnderTest.generateHelperForClassesAllIn(packageName);

        // Assertion
        verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo1.class);
        verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo2.class);
    }

    @Test
    public void testGenerateHelperForClassesAllIn_MatchersHaveBeenGenerated_GeneratedMatcherFilesShouldBeGeneratedAndReturned()
        throws Exception {
        // Preparation
        final Path simplePojo1MatcherPath = mock(Path.class, "simplePojo1MatcherPath");
        final Path simplePojo2MatcherPath = mock(Path.class, "simplePojo2MatcherPath");

        final List<Class<?>> baseClassList = asList(SamplePojo1.class, SamplePojo2.class);
        final List<Class<?>> filteredBaseClassList = asList(SamplePojo1.class,
                SamplePojo2.class);

        doReturn(baseClassList).when(potentialPojoClassFinder).findClasses(packageName);
        doReturn(filteredBaseClassList).when(illegalClassFilter).filter(baseClassList);
        doReturn(simplePojo1MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
            SamplePojo1.class);
        doReturn(simplePojo2MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
            SamplePojo2.class);

        // Execution
        final List<Path> retVal = classUnderTest.generateHelperForClassesAllIn(packageName);

        // Assertion
        assertThat(retVal, is(containsInAnyOrder(simplePojo1MatcherPath, simplePojo2MatcherPath)));
    }

    @Test
    public void testGenerateHelperForClassesAllIn_SomeClassesAreIllegal_GeneratedMatcherFilesShouldOnlyForNonIllegalClassesBeGeneratedAndReturned()
        throws Exception {
        // Preparation
        final Path simplePojo1MatcherPath = mock(Path.class, "simplePojo1MatcherPath");

        final List<Class<?>> baseClassList = asList(SamplePojo1.class, SamplePojo2.class);
        final List<Class<?>> filteredBaseClassList = Collections.singletonList(SamplePojo1.class);

        doReturn(baseClassList).when(potentialPojoClassFinder).findClasses(packageName);
        doReturn(filteredBaseClassList).when(illegalClassFilter).filter(baseClassList);
        doReturn(simplePojo1MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
            SamplePojo1.class);

        // Execution
        final List<Path> retVal = classUnderTest.generateHelperForClassesAllIn(packageName);

        // Assertion
        assertThat(retVal, is(contains(simplePojo1MatcherPath)));
    }

    @Test
    public void test_MixOfErrorCausingClassesAndGoodOnes_ShouldLoadNonErrorCausingClassesOnly()
            throws Exception {
        // Preparation
        final Path simplePojo1MatcherPath = mock(Path.class, "simplePojo1MatcherPath");

        final List<Class<?>> baseClassList = asList(SamplePojo1.class, SamplePojo2.class);
        final List<Class<?>> filteredBaseClassList = asList(SamplePojo1.class,
                SamplePojo2.class);

        doReturn(baseClassList).when(potentialPojoClassFinder).findClasses(packageName);
        doReturn(filteredBaseClassList).when(illegalClassFilter).filter(baseClassList);
        doReturn(simplePojo1MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
                SamplePojo1.class);
        final IOException exception = new IOException("someError");
        doThrow(exception).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
                SamplePojo2.class);

        // Execution
        final List<Path> retVal = classUnderTest.generateHelperForClassesAllIn(packageName);

        // Assertion
        assertThat(retVal, is(contains(simplePojo1MatcherPath)));
    }

    private static class SamplePojo1 {
    }

    private static class SamplePojo2 {
    }
}
