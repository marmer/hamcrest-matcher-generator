package io.github.marmer.testutils.samplepojos;

import java.io.IOException;
import java.nio.file.Path;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

public class HasPropertyMatcherGenerator {

	private static final String POSTFIX = "Matcher";

	public void generateMatcherFor(final Class<?> type, final Path outputDir) throws IOException {
		JavaFile javaFile = prepareType(type);
		javaFile.writeTo(outputDir);
	}

	private JavaFile prepareType(final Class<?> type) {
		TypeSpec typeSpec = prepareClass(type);
		return JavaFile.builder(type.getPackage().getName(), typeSpec).indent("\t").skipJavaLangImports(true).build();
	}

	private TypeSpec prepareClass(final Class<?> type) {
		return TypeSpec.classBuilder(type.getSimpleName() + POSTFIX).addModifiers(Modifier.PUBLIC).build();
	}

}
