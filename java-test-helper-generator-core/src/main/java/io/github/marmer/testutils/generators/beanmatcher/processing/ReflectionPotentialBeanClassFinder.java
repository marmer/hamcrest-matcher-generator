package io.github.marmer.testutils.generators.beanmatcher.processing;

import com.google.common.base.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.reflections.Reflections;

import org.reflections.scanners.SubTypesScanner;

import org.reflections.util.ClasspathHelper;

import java.net.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A {@link PotentialPojoClassFinder} using reflection to find classes.
 *
 * <p>See: https://github.com/ronmamo/reflections</p>
 *
 * @author  marmer
 * @date    18.06.2017
 */
public class ReflectionPotentialBeanClassFinder implements PotentialPojoClassFinder {
	private final Collection<URL> classLoaderURLs;

	public ReflectionPotentialBeanClassFinder(final ClassLoader... classLoaders) {
		this.classLoaderURLs = ClasspathHelper.forManifest(ClasspathHelper.forClassLoader(classLoaders));
	}

	@Override
	public List<Class<?>> findClasses(final String... packageNames) {
		if (ArrayUtils.isEmpty(packageNames)) {
			return Collections.emptyList();
		}

		final Set<Class<?>> packagesSet = new HashSet<>();
		for (final String packageName : packageNames) {
			packagesSet.addAll(findClasses(packageName));
		}

		return packagesSet.parallelStream().filter(this::isRelevant).collect(Collectors.toList());
	}

	private Set<Class<?>> findClasses(final String packageName) {
		if (StringUtils.isBlank(packageName)) {
			return Collections.emptySet();
		}
		final Reflections reflections = new Reflections(packageName,
		        classLoaderURLs, new SubTypesScanner(false));
		return reflections.getSubTypesOf(Object.class);
	}

	private boolean isRelevant(final Class<?> clazz) {
		return !isIrrelevant(clazz);
	}

	private boolean isIrrelevant(final Class<?> clazz) {
		return isPackageInfo(clazz) || clazz.isInterface() || clazz.isAnonymousClass();
	}

	private boolean isPackageInfo(final Class<?> clazz) {
		return Objects.equal("package-info", clazz.getSimpleName());
	}

}
