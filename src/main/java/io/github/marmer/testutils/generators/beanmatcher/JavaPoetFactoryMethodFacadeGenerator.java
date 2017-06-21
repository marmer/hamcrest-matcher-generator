package io.github.marmer.testutils.generators.beanmatcher;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import java.nio.file.Path;

import java.util.List;
import java.util.stream.Collectors;


public class JavaPoetFactoryMethodFacadeGenerator implements FactoryMethodFacadeGenerator {
	private final Path outputPath;
	private final String packageName;
	private final String className;

	public JavaPoetFactoryMethodFacadeGenerator(final Path outputPath, final String packageName,
		final String className) {
		this.outputPath = outputPath;
		this.packageName = packageName;
		this.className = className;

	}

	@Override
	public void generateFacadeFor(final List<Class<?>> classesToGenerateFacadeFor) throws IOException {
		final JavaFile javaFile = JavaFile.builder(packageName, typeFor(classesToGenerateFacadeFor)).build();
		javaFile.writeTo(outputPath);
	}

	private TypeSpec typeFor(final List<Class<?>> classesToGenerateFacadeFor) {
		return TypeSpec.classBuilder(className).addMethods(factoryMethodsFor(classesToGenerateFacadeFor)).build();
	}

	private List<MethodSpec> factoryMethodsFor(final List<Class<?>> classesToGenerateFacadeFor) {
		return classesToGenerateFacadeFor.stream().map(this::factoryMethodsFor).collect(Collectors.toList());
	}

	private MethodSpec factoryMethodsFor(final Class<?> classesToGenerateFacadeFor) {
		return MethodSpec.methodBuilder("is" + StringUtils.capitalize(classesToGenerateFacadeFor.getSimpleName()))
			.build();
	}

}
