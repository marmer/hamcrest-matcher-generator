package io.github.marmer.annotationprocessing

import com.squareup.javapoet.*
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.TypeName.BOOLEAN
import com.squareup.javapoet.TypeName.get
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
import javax.lang.model.type.*


class MatcherGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val baseType: TypeElement,
    private val generationTimeStamp: () -> LocalDateTime,
    private val generationMarker: String,
    private val additionalOriginationElements: Collection<Element>,
    private val matcherConfigurationMirror: AnnotationMirror,
    private val matcherConfiguration: MatcherConfiguration
) {

    fun generate() = JavaFile.builder(
        getBasePackage(),
        getPreparedTypeSpecBuilder()
            .build()
    ).build()
        .writeTo(processingEnv.filer)

    private fun getBasePackage() =
        if (matcherConfiguration.generation.packageConfig.value.isBlank()
        ) baseType.packageElement.toString()
        else "${
            matcherConfiguration.generation.packageConfig.value.replace(
                Regex("\\.+$"),
                ""
            )
        }.${baseType.packageElement}"

    private fun getPreparedTypeSpecBuilder(): TypeSpec.Builder {
        val classBuilder = TypeSpec.classBuilder(simpleMatcherName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(getGeneratedAnnotation())
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
            .flatMap {
                listOfNotNull(
                    it.getHamcrestMatcher(),
                    it.toEqualsMatcher(),
                )
            }

    private fun Property.getHamcrestMatcher() =
        if (type.isMatcher) null
        else methodBuilder("with${name.capitalized}")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(
                toMatcherType(),
                "matcher",
                Modifier.FINAL
            )
            .addStatement(
                "\$L.with(\$S, matcher)",
                builderFieldName,
                name
            )
            .addStatement(
                "return this"
            )
            .returns(getGeneratedTypeName())
            .build()

    private fun Property.toMatcherType() =
        ParameterizedTypeName.get(
            ClassName.get(Matcher::class.java),
            WildcardTypeName.supertypeOf(
                if (type.kind == TypeKind.TYPEVAR) get(Object::class.java)
                else get(boxedType)
            )
        )

    private fun Property.toEqualsMatcher() =
        methodBuilder("with${name.capitalized}")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(
                toEqualsMatcherParameterType(),
                "value", Modifier.FINAL
            )
            .addStatement(
                "\$L.with(\$S, \$T.equalTo(value))", builderFieldName, name,
                Matchers::class.java
            )
            .addStatement("return this")
            .returns(getGeneratedTypeName())
            .build()

    private fun Property.toEqualsMatcherParameterType(): TypeName =
        if (type.kind == TypeKind.TYPEVAR) get(Object::class.java)
        else {
            type.typeVarsToWildcards()
        }

    private fun TypeMirror.typeVarsToWildcards(): TypeName =
        when {
            this is DeclaredType && typeArguments.isNotEmpty() -> {
                ParameterizedTypeName.get(
                    ClassName.get(asTypeElement()),
                    *(typeArguments.map {
                        when {
                            it is TypeVariable -> WildcardTypeName.subtypeOf(Object::class.java)
                            it is WildcardType ->
                                when {
                                    it.extendsBound != null -> WildcardTypeName.subtypeOf(it.extendsBound.typeVarsToWildcards())
                                    it.superBound != null -> WildcardTypeName.supertypeOf(it.superBound.typeVarsToWildcards())
                                    else -> WildcardTypeName.subtypeOf(Object::class.java)
                                }
                            else -> WildcardTypeName.subtypeOf(it.typeVarsToWildcards())
                        }
                    }.toTypedArray())
                )
            }
            this is TypeVariable -> get(Object::class.java)
            else -> get(this)
        }

    private fun getApiInitializer() =
        methodBuilder("is${baseType.simpleName}")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addStatement("return new \$T()", getGeneratedTypeName())
            .returns(getGeneratedTypeName())
            .build()

    private fun getSimpleMatcherClassName() =
        ClassName.get("", simpleMatcherName)

    private fun getGeneratedTypeName() =
        getSimpleMatcherClassName()

    private fun getConstructor() = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addStatement(
            "\$L = new \$T(\$T.class)",
            builderFieldName,
            getBuilderFieldType(),
            processingEnv.typeUtils.getDeclaredType(baseType),
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
        .addParameter(baseType.typeNameWithWildCards, "item", Modifier.FINAL)
        .addStatement("return \$L.matches(\$L)", builderFieldName, "item")
        .returns(BOOLEAN)
        .build()

    private fun getDescribeMissmatchSafelyMethod() =
        methodBuilder("describeMismatchSafely")
            .addAnnotation(Override::class.java)
            .addParameter(baseType.typeNameWithWildCards, "item", Modifier.FINAL)
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
        baseType.typeNameWithWildCards
    )

    private val builderFieldName = "beanPropertyMatcher"

    private fun getSuperClass() = ParameterizedTypeName.get(
        ClassName.get(TypeSafeMatcher::class.java),
        baseType.typeNameWithWildCards
    )

    private fun getInnerMatchers(): List<TypeSpec> {
        val innerMatcherBases = baseType.enclosedElements
            .filterIsInstance(TypeElement::class.java)
            .partition { it.modifiers.contains(Modifier.PUBLIC) }

        innerMatcherBases.second.forEach(this::logTypeSkipped)
        return innerMatcherBases
            .first
            .map {
                MatcherGenerator(
                    processingEnv,
                    it,
                    generationTimeStamp,
                    generationMarker,
                    listOf<Element>(baseType) + additionalOriginationElements,
                    matcherConfigurationMirror,
                    matcherConfiguration
                ).getPreparedTypeSpecBuilder()
                    .addModifiers(Modifier.STATIC)
                    .build()
            }
    }

    private fun logTypeSkipped(element: TypeElement) {
        processingEnv.logNote(
            "Matcher generation skipped for non public type: ${element.qualifiedName}",
            element,
            matcherConfigurationMirror,
            matcherConfigurationMirror.getAnnotationValueForValue()
        )
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
        get() = if (superclass.kind != TypeKind.NONE)
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

    private val simpleMatcherName = "${processingEnv.typeUtils.getDeclaredType(baseType).asElement().simpleName}Matcher"

    private fun TypeMirror.asTypeElement() =
        (processingEnv.typeUtils.asElement(this) as TypeElement)

    private val TypeMirror.isMatcher: Boolean
        get() =
            kind == TypeKind.DECLARED && Matcher::class.java.canonicalName == this.asTypeElement().qualifiedName.toString()

    private val TypeElement.packageElement: PackageElement
        get() = processingEnv.elementUtils.getPackageOf(this)


    private val TypeElement.typeName: TypeName
        get() = get(asType())

    private val TypeElement.typeNameWithWildCards: TypeName
        get() = if (typeParameters.isEmpty())
            typeName
        else
            ParameterizedTypeName.get(
                ClassName.get(this),
                *(this.typeParameters.map { WildcardTypeName.subtypeOf(Object::class.java) }).toTypedArray()
            )

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
