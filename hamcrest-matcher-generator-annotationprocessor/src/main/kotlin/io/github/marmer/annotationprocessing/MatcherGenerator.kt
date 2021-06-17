package io.github.marmer.annotationprocessing

import com.squareup.javapoet.*
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.TypeName.BOOLEAN
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import java.time.LocalDateTime
import javax.annotation.processing.Generated
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class MatcherGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val baseType: TypeElement,
    private val generationTimeStamp: () -> LocalDateTime,
    private val generationMarker: String,
    private val typesWithAsserters: Collection<TypeElement>
) {

    fun generate() = JavaFile.builder(
        baseType.packageElement.toString(),
        getPreparedTypeSpecBuilder()
            .build()
    ).build()
        .writeTo(processingEnv.filer)

    private fun getPreparedTypeSpecBuilder() = TypeSpec.classBuilder(simpleMatcherName)
        .addOriginatingElement(baseType)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(getGeneratedAnnotation())
        .addTypeVariables(baseType.typeParameters.map { TypeVariableName.get(it) })
        .superclass(getSuperClass())
//        .addSuperinterface(getPojoAsserterInterface())
        .addFields(getFields())
        .addMethods(getInitializers())
//        .addMethods(getBaseAssertionMethods())
//        .addMethods(getPropertyAssertionMethods())
//        .addMethods(getFinisherMethods())
        .addMethods(getMatcherMethods())
        .addTypes(getInnerMatchers())

    private fun getInitializers() = listOf(
        getBaseTypeConstructor(),
        getApiInitializer()
    )

    private fun getApiInitializer() =
        methodBuilder("is${baseType.simpleName}") // TODO: marmer 17.06.2021 check if genericy have to be erased
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(baseType.typeParameters.map { TypeVariableName.get(it) })
            .addParameter(baseType.typeName, "base", Modifier.FINAL)
            .addStatement("return new \$T(base)", getGeneratedTypeName())
            .returns(getGeneratedTypeName())
            .build()

    private fun getSimpleMatcherClassName() =
        ClassName.get("", simpleMatcherName)

    private fun getGeneratedTypeName() =
        if (baseType.typeParameters.isEmpty()) ClassName.get("", simpleMatcherName)
        else getSimpleMatcherClassNameWithParameters()

    private fun getSimpleMatcherClassNameWithParameters() = ParameterizedTypeName.get(
        getSimpleMatcherClassName(),
        *(baseType.typeParameters.map { TypeVariableName.get(it) }.toTypedArray())
    )


    private fun getBaseTypeConstructor() = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addStatement(
            "\$L = new \$L(\$L.class)",
            builderFieldName,
            getBuilderFieldType(),
            baseType.simpleName,
        )
        .build()


    private fun getMatcherMethods() = listOf(
        getDescribeToMethod(),
        getMatchesSafelyMethod(),
        getDescribeMissmatchSafelyMethod()
    )

    private fun getDescribeToMethod() = methodBuilder("describeTo")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override::class.java)
        .addParameter(
            Description::class.java,
            "description",
            Modifier.FINAL
        )
        .addStatement(
            "\$L.describeTo(\$L)",
            builderFieldName,
            "description"
        )
        .build()

    private fun getMatchesSafelyMethod() = methodBuilder("matchesSafely")
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(baseType.typeName, "item", Modifier.FINAL)
        .addStatement("return \$L.matches(\$L)", builderFieldName, "item")
        .returns(BOOLEAN)
        .build();

    private fun getDescribeMissmatchSafelyMethod() =
        methodBuilder("describeMismatchSafely")
            .addAnnotation(Override::class.java)
            .addParameter(baseType.typeName, "item", Modifier.FINAL)
            .addParameter(
                Description::class.java,
                "description",
                Modifier.FINAL
            )
            .addStatement(
                "\$L.describeMismatch(\$L, \$L)",
                builderFieldName,
                "item",
                "description"
            )
            .addModifiers(Modifier.PROTECTED).build();


    private fun getFields() = listOf(
        FieldSpec.builder(
            getBuilderFieldType(),
            builderFieldName,
            Modifier.PRIVATE,
            Modifier.FINAL
        ).build()
    )

    private fun getBuilderFieldType() = ParameterizedTypeName.get(
        ClassName.get(BeanPropertyMatcher::class.java),
        baseType.typeName
    )

    private val builderFieldName = "beanPropertyMatcher"

    private fun getSuperClass() = ParameterizedTypeName.get(
        ClassName.get(TypeSafeMatcher::class.java),
        TypeName.get(baseType.asType())
    )

    private fun getInnerMatchers(): List<TypeSpec> =
        baseType.enclosedElements
            .filterIsInstance(TypeElement::class.java)
            .filterNot { it.modifiers.contains(Modifier.PRIVATE) }
            .map {
                MatcherGenerator(
                    processingEnv,
                    it,
                    generationTimeStamp,
                    generationMarker,
                    typesWithAsserters
                ).getPreparedTypeSpecBuilder()
                    .addModifiers(Modifier.STATIC)
                    .build()
            }

    private fun getGeneratedAnnotation() = AnnotationSpec.builder(Generated::class.java)
        .addMember("value", "\$S", generationMarker)
        .addMember("date", "\$S", generationTimeStamp())
        .build()

    private val TypeElement.packageElement: PackageElement
        get() = processingEnv.elementUtils.getPackageOf(this)

    private val simpleMatcherName = "${baseType.simpleName}Matcher"

    private val TypeElement.typeName: TypeName
        get() = TypeName.get(asType())
}

