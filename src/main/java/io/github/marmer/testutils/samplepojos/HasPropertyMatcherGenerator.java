package io.github.marmer.testutils.samplepojos;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.hamcrest.core.IsInstanceOf;

import java.io.IOException;

import java.nio.file.Path;

import javax.annotation.Generated;

import javax.lang.model.element.Modifier;


public class HasPropertyMatcherGenerator {

	private static final String POSTFIX = "Matcher";

	public void generateMatcherFor(final Class<?> type, final Path outputDir) throws IOException {
		final JavaFile javaFile = prepareJavaFile(type);
		javaFile.writeTo(outputDir);
	}

	private JavaFile prepareJavaFile(final Class<?> type) {
		final TypeSpec typeSpec = type(type);

		return JavaFile.builder(type.getPackage().getName(), typeSpec).indent("\t").skipJavaLangImports(true).build();
	}

	private TypeSpec type(final Class<?> type) {
		return TypeSpec.classBuilder(type.getSimpleName() + POSTFIX).superclass(IsInstanceOf.class)
		    .addModifiers(Modifier.PUBLIC).addMethod(constructor(type)).addAnnotation(generatedAnnotation()).build();
	}

	private AnnotationSpec generatedAnnotation() {
		return AnnotationSpec.builder(Generated.class).addMember("value", "$S", getClass().getName()).build();
	}

	private MethodSpec constructor(final Class<?> type) {
		return MethodSpec.constructorBuilder().addStatement("super($T.class)", type).addModifiers(Modifier.PUBLIC)
		    .build();
	}

}
