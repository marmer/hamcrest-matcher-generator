package io.github.marmer.testutils.generators.beanmatcher.processing;

import com.google.common.base.Objects;
import io.github.classgraph.ClassGraph;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A {@link PotentialPojoClassFinder} using reflection to find classes.
 *
 * <p>See: https://github.com/ronmamo/reflections</p>
 *
 * @author  marmer
 * @since   18.06.2017
 */
public class ReflectionPotentialBeanClassFinder implements PotentialPojoClassFinder {
	private final boolean allowInterfaces;
	private final ClassLoader[] classLoaders;
	private final boolean ignoreClassesWithoutProperties;
	private final BeanPropertyExtractor beanPropertyExtractor;

	public ReflectionPotentialBeanClassFinder(final BeanPropertyExtractor beanPropertyExtractor,
											  final boolean ignoreClassesWithoutProperties, final boolean allowInterfaces, final ClassLoader... classLoaders) {
		this.beanPropertyExtractor = beanPropertyExtractor;
		this.ignoreClassesWithoutProperties = ignoreClassesWithoutProperties;
		this.allowInterfaces = allowInterfaces;
		this.classLoaders = classLoaders;
	}

	@Override
	public List<Class<?>> findClasses(final String... packageNames) {
		if (ArrayUtils.isEmpty(packageNames)) {
			return Collections.emptyList();
		}

		// TODO: marmer 19.01.2019 do some experiments with real projects
		// TODO: marmer 19.01.2019 remove irrevant artefacts of the old implementation (as well as dependencies. I'm looking at you "org.refections:reflections")
		// TODO: marmer 19.01.2019 do some refactoring

		final String[] packagenamesWithoutEmptyPackages = Stream.of(packageNames)
				.filter(StringUtils::isNotBlank)
				.toArray(String[]::new);

		if (ArrayUtils.isEmpty(packagenamesWithoutEmptyPackages)) {
			return Collections.emptyList();
		}

		final ClassGraph classGraph = new ClassGraph();
		Stream.of(classLoaders).forEach(classGraph::addClassLoader);

		return classGraph
				.enableAllInfo()
				.whitelistPackages(packagenamesWithoutEmptyPackages)
				.whitelistClasses(packagenamesWithoutEmptyPackages)
				.scan()
				.getAllClasses()
				.loadClasses(true)
				.stream()
				.filter(this::isRelevant)
				.collect(Collectors.toList());
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
		return !allowInterfaces && clazz.isInterface();
	}

	private boolean isPackageInfo(final Class<?> clazz) {
		return Objects.equal("package-info", clazz.getSimpleName());
	}
}
