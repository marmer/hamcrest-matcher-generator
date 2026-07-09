package io.github.marmer.annotationprocessing

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
        val matcherMapType = ParameterizedTypeName.get(
            ClassName.get(java.util.Map::class.java),
            ClassName.get(String::class.java),
            matcherListType
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
                FieldSpec.builder(matcherMapType, "hasPropertyMatcher", Modifier.PRIVATE, Modifier.FINAL)
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
                    .addStatement("fullMatcher.addAll(hasPropertyMatcherToList())")
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
                    .addStatement("addToHasPropertyMatcher(propertyName, buildPropertyMatcher(propertyName, matcher))")
                    .addStatement("return this")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("with")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .returns(selfWithT)
                    .addStatement(
                        "addToHasPropertyMatcher(propertyName, \$T.hasProperty(propertyName))",
                        Matchers::class.java
                    )
                    .addStatement("return this")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("reset")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .addStatement("hasPropertyMatcher.remove(propertyName)")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("buildPropertyMatcher")
                    .addAnnotation(
                        com.palantir.javapoet.AnnotationSpec.builder(SuppressWarnings::class.java)
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
                        "for (final \$T matcher : hasPropertyMatcherToList())",
                        matcherOfAnything
                    )
                    .beginControlFlow("if (!matcher.matches(item))")
                    .beginControlFlow("if (mismatchDescriptionAlreadyAdded)")
                    .addStatement("mismatchDescription.appendText(\$S)", " and ")
                    .endControlFlow()
                    .addStatement("matcher.describeMismatch(item, mismatchDescription)")
                    .addStatement("mismatchDescriptionAlreadyAdded = true")
                    .endControlFlow()
                    .endControlFlow()
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("hasPropertyMatcherToList")
                    .addModifiers(Modifier.PRIVATE)
                    .returns(matcherListType)
                    .addStatement(
                        "return hasPropertyMatcher.values().stream().flatMap(\$T::stream).collect(\$T.toList())",
                        java.util.Collection::class.java,
                        java.util.stream.Collectors::class.java
                    )
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("addToHasPropertyMatcher")
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(String::class.java, "propertyName", Modifier.FINAL)
                    .addParameter(matcherOfAnything, "matcher", Modifier.FINAL)
                    .addStatement(
                        "hasPropertyMatcher.computeIfAbsent(propertyName, key -> new \$T<>()).add(matcher)",
                        java.util.ArrayList::class.java
                    )
                    .build()
            )
            .build()
    }
}
