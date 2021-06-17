package io.github.marmer.annotationprocessing

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
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
//        .addSuperinterface(getPojoAsserterInterface())
//        .addField(getPojoAssertionBuilderField())
//        .addMethods(getInitializers())
//        .addMethods(getBaseAssertionMethods())
//        .addMethods(getPropertyAssertionMethods())
//        .addMethods(getFinisherMethods())
        .addTypes(getInnerMatchers())

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
}

