package io.github.marmer.testutils.samplepojos;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
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


@CommonsLog
public class HasPropertyMatcherGenerator {

	private static final String POSTFIX = "Matcher";

	public void generateMatcherFor(final Class<?> type, final Path outputDir) throws IOException {
		final JavaFile javaFile = prepareJavaFile(type);
		if (log.isDebugEnabled()) {
			log.debug(type);
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
		return TypeSpec.classBuilder(matcherNameFor(type)).superclass(IsInstanceOf.class)
		    .addModifiers(Modifier.PUBLIC).addMethod(constructor(type)).addAnnotation(generatedAnnotation()).addMethods(
		        propertyMethods(type)).addMethods(addTypesafeMatcherMethods(type)).build();
	}

	private Iterable<MethodSpec> addTypesafeMatcherMethods(final Class<?> type) {
		return Arrays.asList(describeToMethod(), matchesSafelyMethod(type), describeMissmatchSafelyMethod(type));
	}

	private MethodSpec describeMissmatchSafelyMethod(final Class<?> type) {
		final String parameterName = "description";
		return MethodSpec.methodBuilder("describeTo").addAnnotation(Override.class).addParameter(Description.class,
		        parameterName, Modifier.FINAL).addStatement(
		        "beanPropertyMatcher.describeTo($L)", parameterName).build();
	}

	private MethodSpec matchesSafelyMethod(final Class<?> type) {
		// TODO Auto-generated method stub
		return MethodSpec.methodBuilder("matchesSafely").addAnnotation(Override.class).build();
	}

	private MethodSpec describeToMethod() {
		// TODO Auto-generated method stub
		return MethodSpec.methodBuilder("describeMismatchSafely").addAnnotation(Override.class).build();
	}

	private Iterable<MethodSpec> propertyMethods(final Class<?> type) {
		final PropertyDescriptor[] propertyDescriptors;
		try {
			propertyDescriptors = Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors();
		} catch (IntrospectionException e) {
			// TODO test me!
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
		        "beanPropertyMatcher.with($S, matcher)", propertyDescriptor.getName()).addStatement(
		        "return this")
		    .build();
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
		        "beanPropertyMatcher = new BeanPropertyMatcher<$T>($T.class)", type, type).addModifiers(Modifier.PUBLIC)
		    .build();
	}

}
