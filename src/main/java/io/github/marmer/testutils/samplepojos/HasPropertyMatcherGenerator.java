package io.github.marmer.testutils.samplepojos;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import io.github.marmer.testutils.BeanPropertyMatcher;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.lang3.StringUtils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import org.hamcrest.beans.HasProperty;

import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsInstanceOf;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import java.io.IOException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Generated;

import javax.lang.model.element.Modifier;


/**
 * This class can be used to generate a compile safe matcher for a property bean which acts as a
 * combination of {@link AllOf}, {@link IsInstanceOf} and {@link HasProperty} like:
 *
 * <pre>
    is(allOf(
        instanceOf(SomeModel.class),
        hasProperty("someProperty", equalTo(42)),
        hasProperty("anotherProperty", greaterThan(42))));
 * </pre>
 *
 * The result can be used as follows.
 *
 * <pre>
    SomeModel.isSomeModel().withSomeProperts(equalTo(42))
                            .withAnotherProperty(greaterThan(42));
 * </pre>
 *
 * When you automate the generation before the compiler starts, the compiler will show you all the
 * locations you matched the property. So you don't have to wait and whatch your tests fail.
 *
 * @author  marmer
 * @date    12.06.2017
 */
@CommonsLog
public class HasPropertyMatcherGenerator {
	private static final String POSTFIX = "Matcher";
	private static final String INNER_MATCHER_FIELD_NAME = "beanPropertyMatcher";

	/**
	 * Generate matcher for the given type in the given class directory (including its package
	 * folders).
	 *
	 * @param   type       the type
	 * @param   outputDir  the output dir
	 *
	 * @throws  IOException  Signals that an I/O exception has occurred.
	 */
	public void generateMatcherFor(final Class<?> type, final Path outputDir) throws IOException {
		final JavaFile javaFile = prepareJavaFile(type);
		if (log.isDebugEnabled()) {
			log.debug(javaFile);
		}
		javaFile.writeTo(outputDir);
	}

	private JavaFile prepareJavaFile(final Class<?> type) {
		return JavaFile.builder(getPackageFor(type), generatedTypeFor(type)).indent("\t").skipJavaLangImports(true)
			.build();
	}

	private String getPackageFor(final Class<?> type) {
		return type.getPackage().getName();
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
					type), "beanPropertyMatcher", Modifier.PRIVATE, Modifier.FINAL).build();
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
		final PropertyDescriptor[] propertyDescriptors;
		try {
			propertyDescriptors = Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors();
		} catch (IntrospectionException e) {
			log.error("Failed to read properties of " + type, e);
			return Collections.emptyList();
		}

		final List<MethodSpec> methods = new ArrayList<>();
		for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			methods.add(propertyMethodFor(propertyDescriptor, type));
		}

		return methods;
	}

	private MethodSpec propertyMethodFor(final PropertyDescriptor propertyDescriptor, final Class<?> type) {
		return MethodSpec.methodBuilder(methodNameToGenerateFor(propertyDescriptor)).returns(
				classNameOfGeneratedTypeFor(
					type))
			.addModifiers(
				Modifier.PUBLIC).addParameter(parameterizedMatchertype(), "matcher", Modifier.FINAL).addStatement(
				"$L.with($S, matcher)", INNER_MATCHER_FIELD_NAME, propertyDescriptor.getName()).addStatement(
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

	private String methodNameToGenerateFor(final PropertyDescriptor propertyDescriptor) {
		return "with" + StringUtils.capitalize(propertyDescriptor.getName());
	}

	private ClassName classNameOfGeneratedTypeFor(final Class<?> type) {
		return ClassName.get(getPackageFor(type), matcherNameFor(type));
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
