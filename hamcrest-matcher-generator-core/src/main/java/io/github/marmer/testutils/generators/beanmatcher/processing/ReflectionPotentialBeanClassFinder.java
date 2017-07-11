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
 * @author marmer
 * @since  18.06.2017
 */
public class ReflectionPotentialBeanClassFinder implements PotentialPojoClassFinder {
    private final Collection<URL> classLoaderURLs;
    private ClassLoader[] classLoaders;

    public ReflectionPotentialBeanClassFinder(final ClassLoader... classLoaders) {
        this.classLoaders = classLoaders;
        this.classLoaderURLs =
            ClasspathHelper.forManifest(ClasspathHelper.forClassLoader(classLoaders));
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

    private Set<Class<? extends Object>> classesByClassLoaderUrlsFor(final String packageName) {
        final Set<Class<? extends Object>> classesByClassLoaderUrls = new HashSet<>();
        classesByClassLoaderUrls.addAll(
            new Reflections(packageName, classLoaderURLs, new SubTypesScanner(false)).getSubTypesOf(
                Object.class));
        classesByClassLoaderUrls.addAll(
            new Reflections(packageName, classLoaderURLs, new SubTypesScanner(false)).getSubTypesOf(
                Exception.class));

        return classesByClassLoaderUrls;
    }

    private Set<Class<? extends Object>> classesByClassloaderFor(final String packageName) {
        final Set<Class<? extends Object>> classesByClassLoader = new HashSet<>();

        classesByClassLoader.addAll(
            new Reflections(packageName, classLoaders, new SubTypesScanner(false)).getSubTypesOf(
                Object.class));
        classesByClassLoader.addAll(
            new Reflections(packageName, classLoaders, new SubTypesScanner(false)).getSubTypesOf(
                Exception.class));

        return classesByClassLoader;
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
