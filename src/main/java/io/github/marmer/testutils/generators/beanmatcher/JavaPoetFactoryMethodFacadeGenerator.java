package io.github.marmer.testutils.generators.beanmatcher;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import java.nio.file.Path;

import java.util.List;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;


@CommonsLog
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
		if (log.isDebugEnabled()) {
			log.debug(javaFile);
		}
		javaFile.writeTo(outputPath);
	}

	private TypeSpec typeFor(final List<Class<?>> classesToGenerateFacadeFor) {
		return TypeSpec.classBuilder(className).addMethods(factoryMethodsFor(classesToGenerateFacadeFor)).build();
	}

	private List<MethodSpec> factoryMethodsFor(final List<Class<?>> classesToGenerateFacadeFor) {
		return classesToGenerateFacadeFor.stream().map(this::factoryMethodsFor).collect(Collectors.toList());
	}

	private MethodSpec factoryMethodsFor(final Class<?> classesToGenerateFacadeFor) {
		return MethodSpec.methodBuilder(methodNameFor(classesToGenerateFacadeFor)).addModifiers(PUBLIC, STATIC)
			.build();
	}

	private String methodNameFor(final Class<?> classesToGenerateFacadeFor) {
		return "is" + StringUtils.capitalize(baseTypeOf(classesToGenerateFacadeFor).getSimpleName());
	}

	private Class<?> baseTypeOf(final Class<?> classesToGenerateFacadeFor) {
		final BasedOn annotation = classesToGenerateFacadeFor.getAnnotation(BasedOn.class);
		return annotation.value();
	}

}
