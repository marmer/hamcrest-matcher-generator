package io.github.marmer.annotationprocessing

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import com.palantir.javapoet.TypeVariableName
import com.palantir.javapoet.WildcardTypeName
import org.hamcrest.Description
import org.hamcrest.FeatureMatcher
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import javax.lang.model.element.Modifier

/**
 * Builds the bean property matching runtime logic as a nested static class, so generated
 * matchers are self-contained and only need Hamcrest at test runtime.
 */
internal object BeanPropertyMatcherTypeFactory {

    const val TYPE_NAME = "BeanPropertyMatcher"

    val unqualifiedClassName: ClassName = ClassName.get("", TYPE_NAME)

    private val expectationClassName: ClassName = ClassName.get("", "Expectation")

    fun create(): TypeSpec {
        val t = TypeVariableName.get("T")
        val matcherOfAnything: TypeName = ParameterizedTypeName.get(
            ClassName.get(Matcher::class.java),
            WildcardTypeName.subtypeOf(Object::class.java)
        )
        val matcherListType = ParameterizedTypeName.get(
            ClassName.get(java.util.List::class.java),
            matcherOfAnything
        )
        val expectationListType = ParameterizedTypeName.get(
            ClassName.get(java.util.List::class.java),
            expectationClassName
        )
        val expectationMapType = ParameterizedTypeName.get(
            ClassName.get(java.util.Map::class.java),
            ClassName.get(String::class.java),
            expectationListType
        )
        val classOfSuperT = ParameterizedTypeName.get(
            ClassName.get(Class::class.java),
            WildcardTypeName.supertypeOf(t)
        )
        val selfWithT: TypeName = ParameterizedTypeName.get(unqualifiedClassName, t)

        return TypeSpec.classBuilder(TYPE_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(t)
            .superclass(ParameterizedTypeName.get(ClassName.get(TypeSafeMatcher::class.java), t))
            .addField(
                FieldSpec.builder(expectationMapType, "expectations", Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new \$T<>()", java.util.LinkedHashMap::class.java)
                    .build()
            )
            .addField(
                FieldSpec.builder(matcherOfAnything, "instanceOfMatcher", Modifier.PRIVATE, Modifier.FINAL)
                    .build()
            )
            .addField(
                FieldSpec.builder(classOfSuperT, "expectedClass", Modifier.PRIVATE, Modifier.FINAL)
                    .build()
            )
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(classOfSuperT, "expectedClass", Modifier.FINAL)
                    .addStatement("this.expectedClass = expectedClass")
                    .addStatement("instanceOfMatcher = \$T.instanceOf(expectedClass)", Matchers::class.java)
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("describeTo")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Description::class.java, "description", Modifier.FINAL)
                    .addStatement("getFullInnerMatcher().describeTo(description)")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("matchesSafely")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(t, "item", Modifier.FINAL)
                    .returns(TypeName.BOOLEAN)
                    .addStatement("return getFullInnerMatcher().matches(item)")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("getFullInnerMatcher")
                    .addModifiers(Modifier.PRIVATE)
                    .returns(matcherOfAnything)
                    .addStatement(
                        "final \$T fullMatcher = new \$T<>()",
                        matcherListType,
                        java.util.ArrayList::class.java
                    )
                    .addStatement("fullMatcher.add(instanceOfMatcher)")
                    .beginControlFlow("for (final \$T expectation : expectationsToList())", expectationClassName)
                    .addStatement("fullMatcher.add(expectation.propertyMatcher)")
                    .endControlFlow()
                    .addStatement(
                        "return \$T.allOf(fullMatcher.toArray(new \$T[0]))",
                        Matchers::class.java,
                        Matcher::class.java
                    )
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("with")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .addParameter(matcherOfAnything, "matcher", Modifier.FINAL)
                    .returns(selfWithT)
                    .addStatement(
                        "addExpectation(new \$T(propertyName, matcher, buildPropertyMatcher(propertyName, matcher)))",
                        expectationClassName
                    )
                    .addStatement("return this")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("with")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .returns(selfWithT)
                    .addStatement(
                        "addExpectation(new \$T(propertyName, null, \$T.hasProperty(propertyName)))",
                        expectationClassName,
                        Matchers::class.java
                    )
                    .addStatement("return this")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("reset")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .addStatement("expectations.remove(propertyName)")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("buildPropertyMatcher")
                    .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings::class.java)
                            .addMember("value", "\$S", "unchecked")
                            .build()
                    )
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .addParameter(matcherOfAnything, "valueMatcher", Modifier.FINAL)
                    .returns(matcherOfAnything)
                    .addStatement(
                        "final \$T capitalized = \$T.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1)",
                        String::class.java,
                        Character::class.java
                    )
                    .addStatement(
                        "final boolean hasGetter = hasAccessibleMethod(\$S + capitalized) || hasAccessibleMethod(\$S + capitalized)",
                        "get",
                        "is"
                    )
                    .beginControlFlow("if (!hasGetter && hasAccessibleMethod(propertyName))")
                    .addCode(
                        """
                        return new ${'$'}T<T, ${'$'}T>((${'$'}T<? super ${'$'}T>) valueMatcher, propertyName, propertyName) {
                            @${'$'}T
                            protected ${'$'}T featureValueOf(final T actual) {
                                try {
                                    return actual.getClass().getMethod(propertyName).invoke(actual);
                                } catch (final ${'$'}T e) {
                                    throw new ${'$'}T("Could not read component '" + propertyName + "'", e);
                                }
                            }
                        };

                        """.trimIndent(),
                        FeatureMatcher::class.java,
                        Object::class.java,
                        Matcher::class.java,
                        Object::class.java,
                        Override::class.java,
                        Object::class.java,
                        ReflectiveOperationException::class.java,
                        AssertionError::class.java
                    )
                    .endControlFlow()
                    .addStatement(
                        "return \$T.hasProperty(propertyName, valueMatcher)",
                        Matchers::class.java
                    )
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("hasAccessibleMethod")
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(String::class.java, "methodName", Modifier.FINAL)
                    .returns(TypeName.BOOLEAN)
                    .beginControlFlow("try")
                    .addStatement(
                        "return expectedClass.getMethod(methodName).getParameterCount() == 0"
                    )
                    .nextControlFlow("catch (final \$T e)", NoSuchMethodException::class.java)
                    .addStatement("return false")
                    .endControlFlow()
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("describeMismatchSafely")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(t, "item", Modifier.FINAL)
                    .addParameter(Description::class.java, "mismatchDescription", Modifier.FINAL)
                    .addStatement("boolean mismatchDescriptionAlreadyAdded = false")
                    .beginControlFlow("if (!instanceOfMatcher.matches(item))")
                    .addStatement("mismatchDescription.appendText(\$S + item.getClass())", "Is an instance of ")
                    .addStatement("mismatchDescriptionAlreadyAdded = true")
                    .endControlFlow()
                    .beginControlFlow(
                        "for (final \$T expectation : expectationsToList())",
                        expectationClassName
                    )
                    .beginControlFlow("if (!expectation.propertyMatcher.matches(item))")
                    .beginControlFlow("if (mismatchDescriptionAlreadyAdded)")
                    .addStatement("mismatchDescription.appendText(\$S)", "\n")
                    .endControlFlow()
                    .addStatement("mismatchDescription.appendText(expectation.property + \$S)", ": ")
                    .addStatement("describePropertyMismatch(item, expectation, mismatchDescription)")
                    .addStatement("mismatchDescriptionAlreadyAdded = true")
                    .endControlFlow()
                    .endControlFlow()
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("describePropertyMismatch")
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(t, "item", Modifier.FINAL)
                    .addParameter(expectationClassName, "expectation", Modifier.FINAL)
                    .addParameter(Description::class.java, "mismatchDescription", Modifier.FINAL)
                    .beginControlFlow("if (expectation.valueMatcher == null)")
                    .addStatement("mismatchDescription.appendText(\$S)", "expected a readable property but ")
                    .addStatement("expectation.propertyMatcher.describeMismatch(item, mismatchDescription)")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("mismatchDescription.appendText(\$S)", "expected ")
                    .addStatement("expectation.valueMatcher.describeTo(mismatchDescription)")
                    .addStatement("mismatchDescription.appendText(\$S)", " but ")
                    .beginControlFlow("try")
                    .addStatement(
                        "expectation.valueMatcher.describeMismatch(readProperty(item, expectation.property), mismatchDescription)"
                    )
                    .nextControlFlow("catch (final \$T e)", ReflectiveOperationException::class.java)
                    .addStatement("expectation.propertyMatcher.describeMismatch(item, mismatchDescription)")
                    .endControlFlow()
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("readProperty")
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(t, "item", Modifier.FINAL)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .returns(ClassName.get(Object::class.java))
                    .addException(ClassName.get(ReflectiveOperationException::class.java))
                    .addStatement(
                        "final \$T capitalized = \$T.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1)",
                        String::class.java,
                        Character::class.java
                    )
                    .beginControlFlow(
                        "for (final \$T candidate : new \$T[]{\$S + capitalized, \$S + capitalized, propertyName})",
                        String::class.java,
                        String::class.java,
                        "get",
                        "is"
                    )
                    .beginControlFlow("try")
                    .addStatement("return item.getClass().getMethod(candidate).invoke(item)")
                    .nextControlFlow("catch (final \$T e)", NoSuchMethodException::class.java)
                    .addComment("try next accessor candidate")
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("throw new \$T(propertyName)", NoSuchMethodException::class.java)
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("expectationsToList")
                    .addModifiers(Modifier.PRIVATE)
                    .returns(expectationListType)
                    .addStatement(
                        "return expectations.values().stream().flatMap(\$T::stream).collect(\$T.toList())",
                        java.util.Collection::class.java,
                        java.util.stream.Collectors::class.java
                    )
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("addExpectation")
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(expectationClassName, "expectation", Modifier.FINAL)
                    .addStatement(
                        "expectations.computeIfAbsent(expectation.property, key -> new \$T<>()).add(expectation)",
                        java.util.ArrayList::class.java
                    )
                    .build()
            )
            .addType(
                TypeSpec.classBuilder("Expectation")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addField(
                        FieldSpec.builder(String::class.java, "property", Modifier.PRIVATE, Modifier.FINAL)
                            .build()
                    )
                    .addField(
                        FieldSpec.builder(matcherOfAnything, "valueMatcher", Modifier.PRIVATE, Modifier.FINAL)
                            .build()
                    )
                    .addField(
                        FieldSpec.builder(matcherOfAnything, "propertyMatcher", Modifier.PRIVATE, Modifier.FINAL)
                            .build()
                    )
                    .addMethod(
                        MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .addParameter(String::class.java, "property", Modifier.FINAL)
                            .addParameter(matcherOfAnything, "valueMatcher", Modifier.FINAL)
                            .addParameter(matcherOfAnything, "propertyMatcher", Modifier.FINAL)
                            .addStatement("this.property = property")
                            .addStatement("this.valueMatcher = valueMatcher")
                            .addStatement("this.propertyMatcher = propertyMatcher")
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
