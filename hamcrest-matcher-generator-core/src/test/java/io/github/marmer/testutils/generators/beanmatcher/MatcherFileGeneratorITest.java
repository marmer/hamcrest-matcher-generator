package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.PlainMatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.processing.*;
import io.github.marmer.testutils.utils.matchers.GeneratedFileCompiler;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import sample.classes.SimpleSampleClass;
import sample2.classes.SimplePojo;
import sample2.classes.inheritance.SomeSubClass;
import sample2.classes.inheritance.SomeSuperClass;
import sample2.classes.inheritance.SomeSuperSuperClass;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;


public class MatcherFileGeneratorITest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();
    private final ClassLoader classLoader = getClass().getClassLoader();
    private PotentialPojoClassFinder potentialPojoClassFinder;
    private MatcherGenerator classUnderTest;
    private HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator;
    private Path srcOutputDir;
    private Path classOutputDir;
    private GeneratedFileCompiler compiler;

    @Before
    public void setUp() throws Exception {
        prepareSourceOutputDir();
        prepareOutputDir();
        initCompiler();
        initClassUnderTest();
    }

    private void initClassUnderTest() {
        final Log logger = mock(Log.class);
        final BeanPropertyExtractor propertyExtractor = new IntrospektorBeanPropertyExtractor(new IntrospectorDelegate(), logger);
        potentialPojoClassFinder = new ReflectionPotentialBeanClassFinder(propertyExtractor, false, false);

        hasPropertyMatcherClassGenerator =
                new JavaPoetHasPropertyMatcherClassGenerator(propertyExtractor, srcOutputDir, new PlainMatcherNamingStrategy());
        classUnderTest =
            new MatcherFileGenerator(potentialPojoClassFinder,
                hasPropertyMatcherClassGenerator,
                    new JavaInternalIllegalClassFilter(), logger);
    }

    public void prepareOutputDir() throws Exception {
        this.classOutputDir = temp.newFolder("target").toPath();
    }

    public void prepareSourceOutputDir() throws Exception {
        this.srcOutputDir = temp.newFolder("src").toPath();
    }

    private void initCompiler() {
        compiler = new GeneratedFileCompiler(srcOutputDir, classOutputDir);
    }

    @Test
    public void testGenerateHelperForClassesAllIn_GeneratedInstanceHasMatcherSetAndNotMatchingValueIsGiven_ShouldNotMatch()
        throws Exception {
        // Preparation
        final Class<SimpleSampleClass> type = SimpleSampleClass.class;
        classUnderTest.generateHelperForClassesAllIn(type.getName());

        final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(
                type);
        MethodUtils.invokeMethod(matcher, "withSomeProperty", equalTo("someValue"));

        // Execution
        final boolean matches = matcher.matches(new SimpleSampleClass("someOtherValue"));

        // Assertion
        assertThat("Matcher matches matching class", matches, is(false));
    }

    @Test
    public void testGenerateHelperForClassesAllIn_GeneratedInstanceHasMatcherSetAndMatchingValueIsGiven_ShouldMatch()
        throws Exception {
        // Preparation
        final Class<SimpleSampleClass> type = SimpleSampleClass.class;
        classUnderTest.generateHelperForClassesAllIn(type.getName());

        final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(
                type);
        MethodUtils.invokeMethod(matcher, "withSomeProperty", equalTo("someValue"));

        // Execution
        final boolean matches = matcher.matches(new SimpleSampleClass("someValue"));

        // Assertion
        assertThat("Matcher matches matching class", matches, is(true));
    }

    @Test
    public void testGenerateHelperForClassesAllIn_GeneratedInstanceHasMatcherSetAndNotEqualValueIsGiven_ShouldNotMatch()
        throws Exception {
        // Preparation
        final Class<SimpleSampleClass> type = SimpleSampleClass.class;
        classUnderTest.generateHelperForClassesAllIn(type.getName());

        final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(
                type);
        MethodUtils.invokeMethod(matcher, "withSomeProperty", "someValue");

        // Execution
        final boolean matches = matcher.matches(new SimpleSampleClass("someOtherValue"));

        // Assertion
        assertThat("Matcher matches matching class", matches, is(false));
    }

    @Test
    public void testGenerateHelperForClassesAllIn_GeneratedInstanceHasMatcherSetAndEqualValueIsGiven_ShouldMatch()
        throws Exception {
        // Preparation
        final Class<SimpleSampleClass> type = SimpleSampleClass.class;
        classUnderTest.generateHelperForClassesAllIn(type.getName());

        final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(
                type);
        MethodUtils.invokeMethod(matcher, "withSomeProperty", "someValue");

        // Execution
        final boolean matches = matcher.matches(new SimpleSampleClass("someValue"));

        // Assertion
        assertThat("Matcher matches matching class", matches, is(true));
    }

    @Test
    public void testGenerateMatcherFor_GeneratedInstanceMatcherSettingMethodIsCalled_MethodShouldReturnIstanceOfItselfForConcatenationAbility()
        throws Exception {
        // Preparation
        final Class<?> type = SimpleSampleClass.class;
        classUnderTest.generateHelperForClassesAllIn(type.getName());

        final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(
                type);

        // Execution
        final Object result = MethodUtils.invokeMethod(matcher,
                "withSomeProperty",
                equalTo("someValue"));

        // Assertion
        assertThat(result, is(sameInstance(matcher)));
    }

    @Test
    public void testGenerateHelperForClassesAllIn_GenerationSingleInheritingClass_ClassShouldBeGenerated()
            throws Exception {
        // Preparation
        final Class<SomeSubClass> type = SomeSubClass.class;

        // Execution
        classUnderTest.generateHelperForClassesAllIn(type.getName());

        // Assertion
        final Matcher<SimplePojo> matcher = compiler.compileAndLoadInstanceOfGeneratedClassFor(
                type);
        assertThat(matcher, is(notNullValue()));
    }

    @Test
    public void testGenerateHelperForClassesAllIn_PackageWithSubAndSupertypesGiven_AllClassesShuoldBeGenerated()
            throws Exception {
        // Preparation
        final Class<SomeSubClass> subType = SomeSubClass.class;
        final Class<SomeSuperClass> superType = SomeSuperClass.class;
        final Class<SomeSuperSuperClass> superSuperType = SomeSuperSuperClass.class;

        // Execution
        classUnderTest.generateHelperForClassesAllIn(subType.getPackage().getName());

        // Assertion
        assertThat(compiler.<Matcher<SimplePojo>>compileAndLoadInstanceOfGeneratedClassFor(subType),
                is(notNullValue()));
        assertThat(compiler.<Matcher<SomeSuperClass>>compileAndLoadInstanceOfGeneratedClassFor(superType),
                is(notNullValue()));
        assertThat(compiler.<Matcher<SomeSuperSuperClass>>compileAndLoadInstanceOfGeneratedClassFor(superSuperType),
                is(notNullValue()));
    }

    @Test
    public void testGenerateHelperForClassesAllIn_MultipleEecutionsWithoutRemovingThePreveouselyGeneratedClassesBefore_AllClassesShuoldStillBeGenerated()
            throws Exception {
        // Preparation
        final Class<SomeSubClass> subType = SomeSubClass.class;
        final Class<SomeSuperClass> superType = SomeSuperClass.class;
        final Class<SomeSuperSuperClass> superSuperType = SomeSuperSuperClass.class;

        // Execution
        classUnderTest.generateHelperForClassesAllIn(subType.getPackage().getName());
        classUnderTest.generateHelperForClassesAllIn(subType.getPackage().getName());

        // Assertion
        assertThat(compiler.<Matcher<SimplePojo>>compileAndLoadInstanceOfGeneratedClassFor(subType),
                is(notNullValue()));
        assertThat(compiler.<Matcher<SomeSuperClass>>compileAndLoadInstanceOfGeneratedClassFor(superType),
                is(notNullValue()));
        assertThat(compiler.<Matcher<SomeSuperSuperClass>>compileAndLoadInstanceOfGeneratedClassFor(superSuperType),
                is(notNullValue()));
    }
}
