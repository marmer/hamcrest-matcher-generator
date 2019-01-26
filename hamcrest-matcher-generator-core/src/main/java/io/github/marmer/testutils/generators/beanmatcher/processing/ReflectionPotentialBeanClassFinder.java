package io.github.marmer.testutils.generators.beanmatcher.processing;

import com.google.common.base.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;


/**
 * A {@link PotentialPojoClassFinder} using reflection to find classes.
 *
 * <p>See: https://github.com/ronmamo/reflections</p>
 *
 * @author  marmer
 * @since   18.06.2017
 */
public class ReflectionPotentialBeanClassFinder implements PotentialPojoClassFinder {
	private final Collection<URL> classLoaderURLs;
	private final boolean allowInterfaecs;
	private final ClassLoader[] classLoaders;
	private final boolean ignoreClassesWithoutProperties;
	private final BeanPropertyExtractor beanPropertyExtractor;

	public ReflectionPotentialBeanClassFinder(final BeanPropertyExtractor beanPropertyExtractor,
											  final boolean ignoreClassesWithoutProperties, final boolean allowInterfaecs, final ClassLoader... classLoaders) {
		this.beanPropertyExtractor = beanPropertyExtractor;
		this.ignoreClassesWithoutProperties = ignoreClassesWithoutProperties;
		this.allowInterfaecs = allowInterfaecs;
		this.classLoaders = classLoaders;
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

		return classesFor(packageName);
	}

	private Set<Class<?>> classesFor(final String packageName) {
		final Set<Class<?>> results = new HashSet<>();

		results.addAll(classesByClassloaderFor(packageName));
		results.addAll(classesByClassLoaderUrlsFor(packageName));

		return results;
	}

	private Set<Class<?>> classesByClassLoaderUrlsFor(final String packageNameOrClassName) {
		return classesFor(packageNameOrClassName, this.classLoaderURLs);
	}

	private Set<Class<?>> classesByClassloaderFor(final String packageNameOrClassName) {
		return classesFor(packageNameOrClassName, this.classLoaders);
	}

	private Set<Class<?>> classesFor(final String packageNameOrClassName, final Object classLoaderURLs) {
		final Set<Class<?>> classesByClassLoaderUrls = new HashSet<>();
		final Reflections reflections = new Reflections(packageNameOrClassName, classLoaderURLs, new SubTypesScanner(false));

		try {
			classesByClassLoaderUrls.addAll(singletonList(Class.forName(packageNameOrClassName)));
		} catch (final ClassNotFoundException e) {
			//This will happen if a packages was given and is handled in the following lines
		}

		classesByClassLoaderUrls.addAll(reflections
				.getSubTypesOf(
						Object.class));
		classesByClassLoaderUrls.addAll(reflections
				.getSubTypesOf(
						Exception.class));

		return classesByClassLoaderUrls;
	}

	private boolean isRelevant(final Class<?> clazz) {
		return !isIrrelevantJavaArtifact(clazz) &&
			(!ignoreClassesWithoutProperties || hasProperties(clazz));
	}

	private boolean hasProperties(final Class<?> clazz) {
		return !CollectionUtils.isEmpty(beanPropertyExtractor.getPropertiesOf(clazz));
	}

	private boolean isIrrelevantJavaArtifact(final Class<?> clazz) {
		return isPackageInfo(clazz) ||
				isNotAllowedInterface(clazz) ||
				clazz.isAnonymousClass();
	}

	private boolean isNotAllowedInterface(final Class<?> clazz) {
		return !allowInterfaecs && clazz.isInterface();
	}

	private boolean isPackageInfo(final Class<?> clazz) {
		return Objects.equal("package-info", clazz.getSimpleName());
	}
}
