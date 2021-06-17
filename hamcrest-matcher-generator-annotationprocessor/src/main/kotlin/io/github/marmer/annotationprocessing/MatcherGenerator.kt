package io.github.marmer.annotationprocessing

import com.squareup.javapoet.*
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.TypeName.BOOLEAN
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import java.time.LocalDateTime
import java.util.*
import javax.annotation.processing.Generated
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


class MatcherGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val baseType: TypeElement,
    private val generationTimeStamp: () -> LocalDateTime,
    private val generationMarker: String,
    private val typesWithAsserters: Collection<TypeElement>,
    private val additionalOriginationElements: Collection<Element>
) {

    fun generate() = JavaFile.builder(
        baseType.packageElement.toString(),
        getPreparedTypeSpecBuilder()
            .build()
    ).build()
        .writeTo(processingEnv.filer)

    private fun getPreparedTypeSpecBuilder(): TypeSpec.Builder {
        val classBuilder = TypeSpec.classBuilder(simpleMatcherName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(getGeneratedAnnotation())
            .addTypeVariables(baseType.typeParameters.map { TypeVariableName.get(it) })
            .superclass(getSuperClass())
            .addFields(getFields())
            .addMethod(getConstructor())
            .addMethods(getPropertyMatcherMethods())
            .addMethods(getMatcherMethods())
            .addMethod(getApiInitializer())
            .addTypes(getInnerMatchers())
            .addOriginatingElement(baseType)

        additionalOriginationElements.forEach { classBuilder.addOriginatingElement(it) }

        return classBuilder
    }

    private fun getPropertyMatcherMethods() =
        baseType.properties
            .flatMap { property ->
                listOfNotNull(
                    getHamcrestMatcher(property),
                    getEqualsMatcher(property),
                )
            }

    private fun getHamcrestMatcher(property: Property) =
        methodBuilder("with${property.name.capitalized}")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(
                ParameterizedTypeName.get(
                    ClassName.get(Matcher::class.java),
                    WildcardTypeName.supertypeOf(TypeName.get(property.boxedType))
                ),
                "matcher",
                Modifier.FINAL
            )
            .addStatement(
                "\$L.with(\$S, matcher)",
                builderFieldName,
                property.name
            )
            .addStatement(
                "return this"
            )
            .returns(getGeneratedTypeName())
            .build()

    private fun getEqualsMatcher(property: Property) =
        methodBuilder("with${property.name.capitalized}")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(property.type), "value", Modifier.FINAL)
            .addStatement(
                "\$L.with(\$S, \$T.equalTo(value))", builderFieldName, property.name,
                Matchers::class.java
            )
            .addStatement("return this")
            .returns(getGeneratedTypeName())
            .build()

    private fun getApiInitializer() =
        methodBuilder("is${baseType.simpleName}")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(baseType.typeParameters.map { TypeVariableName.get(it) })
            .addStatement("return new \$T()", getGeneratedTypeName())
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


    private fun getConstructor() = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addStatement(
            "\$L = new \$T(\$T.class)",
            builderFieldName,
            getBuilderFieldType(),
            baseType.typeName,
        )
        .build()


    private fun getMatcherMethods() = listOf(
        getDescribeToMethod(),
        getMatchesSafelyMethod(),
        getDescribeMissmatchSafelyMethod()
    )

    private fun getDescribeToMethod() = methodBuilder("describeTo")
        .addModifiers(Modifier.PUBLIC)
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
        .build()

    private fun getDescribeMissmatchSafelyMethod() =
        methodBuilder("describeMismatchSafely")
            .addAnnotation(Override::class.java)
            .addParameter(baseType.typeName, "item", Modifier.FINAL)
            .addParameter(Description::class.java, "description", Modifier.FINAL)
            .addStatement(
                "\$L.describeMismatch(\$L, \$L)",
                builderFieldName,
                "item",
                "description"
            )
            .addModifiers(Modifier.PROTECTED).build()


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
                    typesWithAsserters,
                    listOf<Element>(baseType) + additionalOriginationElements
                ).getPreparedTypeSpecBuilder()
                    .addModifiers(Modifier.STATIC)
                    .build()
            }

    private val TypeElement.properties: List<Property>
        get() = transitiveInheritedElements
            .filter { it.isProperty }
            .distinctBy { it.simpleName }
            .map { it as ExecutableElement }
            .map {
                Property(
                    name = it.simpleName.withoutPropertyPrefix(),
                    type = it.returnType,
                    accessor = it.toString()
                )
            }

    private val TypeElement.transitiveInheritedElements: List<Element>
        get() = if (superclass.kind != TypeKind.NONE && kind != ElementKind.ENUM)
            enclosedElements +
                    superclass.asTypeElement().transitiveInheritedElements +
                    interfaces.flatMap { it.asTypeElement().transitiveInheritedElements }
        else
            enclosedElements +
                    interfaces.flatMap { it.asTypeElement().transitiveInheritedElements }
    private val Property.boxedType: TypeMirror
        get() =
            if (type is PrimitiveType) processingEnv.typeUtils.boxedClass(type).asType()
            else type

    private fun getGeneratedAnnotation() = AnnotationSpec.builder(Generated::class.java)
        .addMember("value", "\$S", generationMarker)
        .addMember("date", "\$S", generationTimeStamp())
        .build()

    private val simpleMatcherName = "${baseType.simpleName}Matcher"

    private fun TypeMirror.asTypeElement() =
        (processingEnv.typeUtils.asElement(this) as TypeElement)

    private val TypeElement.packageElement: PackageElement
        get() = processingEnv.elementUtils.getPackageOf(this)


    private val TypeElement.typeName: TypeName
        get() = TypeName.get(asType())

    private val Element.isProperty
        get() =
            this is ExecutableElement &&
                    isPublic &&
                    !isStatic &&
                    hasReturnTypeWithMatchingPropertyPrefix() &&
                    hasNoParameters()

    private fun ExecutableElement.hasReturnTypeWithMatchingPropertyPrefix() =
        hasReturnType() &&
                simpleName.startsWith("get") && returnType.kind != TypeKind.BOOLEAN ||
                (simpleName.startsWith("is") && returnType.kind == TypeKind.BOOLEAN)

    private fun ExecutableElement.hasReturnType() =
        returnType.kind != TypeKind.VOID

    private fun ExecutableElement.hasNoParameters() =
        this.parameters.isEmpty()

    private fun Name.withoutPropertyPrefix() = toString()
        .replaceFirst(Regex("^((get)|(is))"), "")
        .decapitalized

    private val String.capitalized: String
        get() = replaceFirstChar { it.titlecase(Locale.getDefault()) }

    private val String.decapitalized: String
        get() = replaceFirstChar { it.lowercase(Locale.getDefault()) }
}

private data class Property(val name: String, val type: TypeMirror, val accessor: String)
