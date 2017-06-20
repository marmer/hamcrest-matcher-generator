package io.github.marmer.testutils.generators.beanmatcher;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.lang3.StringUtils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.IOException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import javax.lang.model.element.Modifier;


/**
 * The Class JavaPoetHasPropertyMatcherClassGenerator.
 *
 * @author  marmer
 * @date    17.06.2017
 */
@CommonsLog
public class JavaPoetHasPropertyMatcherClassGenerator implements HasPropertyMatcherClassGenerator {
	private static final String POSTFIX = "Matcher";
	private static final String INNER_MATCHER_FIELD_NAME = "beanPropertyMatcher";
	private final BeanPropertyExtractor propertyExtractor;
	private final Path outputDir;

	/**
	 * Creates a new Instance.
	 *
	 * @param  propertyExtractor  the property extractor
	 * @param  outputDir          the output dir
	 */
	public JavaPoetHasPropertyMatcherClassGenerator(final BeanPropertyExtractor propertyExtractor,
		final Path outputDir) {
		this.propertyExtractor = propertyExtractor;
		this.outputDir = outputDir;
	}

	/* (non-Javadoc)
	 * @see io.github.marmer.testutils.generators.beanmatcher.HasPropertyMatcherClassGenerator#generateMatcherFor(java.lang.Class, java.nio.file.Path)
	 */
	@Override
	public Path generateMatcherFor(final Class<?> type) throws IOException {
		final JavaFile javaFile = prepareJavaFile(type);
		if (log.isDebugEnabled()) {
			log.debug(javaFile);
		}
		javaFile.writeTo(outputDir);
		return outputDir.resolve(javaFile.toJavaFileObject().getName());
	}

	private JavaFile prepareJavaFile(final Class<?> type) {
		return JavaFile.builder(type.getPackage().getName(), generatedTypeFor(type)).indent("\t").skipJavaLangImports(
				true)
			.build();
	}

	private String matcherNameFor(final Class<?> type) {
		return type.getSimpleName() + POSTFIX;
	}

	private TypeSpec generatedTypeFor(final Class<?> type) {
		return TypeSpec.classBuilder(matcherNameFor(type)).addModifiers(Modifier.PUBLIC).superclass(
				parameterizedTypesafeMatchertype(type)).addField(innerMatcherField(type))
			.addMethod(constructor(type)).addAnnotation(generatedAnnotation()).addMethods(
				propertyMethods(type)).addMethods(addTypesafeMatcherMethods(type)).build();
	}

	private FieldSpec innerMatcherField(final Class<?> type) {
		return FieldSpec.builder(ParameterizedTypeName.get(BeanPropertyMatcher.class,
					type), INNER_MATCHER_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL).build();
	}

	private Iterable<MethodSpec> addTypesafeMatcherMethods(final Class<?> type) {
		return Arrays.asList(describeToMethod(), matchesSafelyMathod(type), describeMismatchSafelyMethod(type));
	}

	private MethodSpec describeToMethod() {
		final String parameterName = "description";
		return MethodSpec.methodBuilder("describeTo").addAnnotation(Override.class).addParameter(Description.class,
				parameterName, Modifier.FINAL).addStatement(
				"$L.describeTo($L)", INNER_MATCHER_FIELD_NAME, parameterName).addModifiers(Modifier.PUBLIC).build();
	}

	private MethodSpec matchesSafelyMathod(final Class<?> type) {
		final String parameterItem = "item";
		return MethodSpec.methodBuilder("matchesSafely").addAnnotation(Override.class).addModifiers(Modifier.PROTECTED)
			.returns(
				Boolean.TYPE).addParameter(type,
				parameterItem, Modifier.FINAL).addStatement(
				"return $L.matches($L)", INNER_MATCHER_FIELD_NAME, parameterItem).build();
	}

	private MethodSpec describeMismatchSafelyMethod(final Class<?> type) {
		final String parameterName = "item";
		final String parameterNameDescription = "description";
		return MethodSpec.methodBuilder("describeMismatchSafely").addAnnotation(Override.class).addParameter(type,
				parameterName, Modifier.FINAL).addStatement(
				"$L.describeMismatch($L, $L)", INNER_MATCHER_FIELD_NAME, parameterName, parameterNameDescription)
			.addParameter(
				Description.class,
				parameterNameDescription, Modifier.FINAL).addModifiers(Modifier.PROTECTED).build();
	}

	private Iterable<MethodSpec> propertyMethods(final Class<?> type) {

		final List<MethodSpec> methods = new ArrayList<>();
		for (final BeanProperty property : propertyExtractor.getPropertiesOf(type)) {
			methods.add(propertyMethodFor(property.getName(), type));
		}

		return methods;
	}

	private MethodSpec propertyMethodFor(final String propertyName, final Class<?> type) {
		return MethodSpec.methodBuilder(methodNameToGenerateFor(propertyName)).returns(
				classNameOfGeneratedTypeFor(
					type))
			.addModifiers(
				Modifier.PUBLIC).addParameter(parameterizedMatchertype(), "matcher", Modifier.FINAL).addStatement(
				"$L.with($S, matcher)", INNER_MATCHER_FIELD_NAME, propertyName).addStatement(
				"return this")
			.build();
	}

	private ParameterizedTypeName parameterizedTypesafeMatchertype(final Class<?> type) {
		return ParameterizedTypeName.get(TypeSafeMatcher.class,
				type);
	}

	private ParameterizedTypeName parameterizedMatchertype() {
		return ParameterizedTypeName.get(ClassName.get(Matcher.class),
				WildcardTypeName.subtypeOf(TypeName.OBJECT));
	}

	private String methodNameToGenerateFor(final String propertyName) {
		return "with" + StringUtils.capitalize(propertyName);
	}

	private ClassName classNameOfGeneratedTypeFor(final Class<?> type) {
		return ClassName.get(type.getPackage().getName(), matcherNameFor(type));
	}

	private AnnotationSpec generatedAnnotation() {
		return AnnotationSpec.builder(Generated.class).addMember("value", "$S", getClass().getName()).build();
	}

	private MethodSpec constructor(final Class<?> type) {
		return MethodSpec.constructorBuilder().addStatement(
				"$L = new BeanPropertyMatcher<$T>($T.class)", INNER_MATCHER_FIELD_NAME, type, type).addModifiers(
				Modifier.PUBLIC)
			.build();
	}

}
