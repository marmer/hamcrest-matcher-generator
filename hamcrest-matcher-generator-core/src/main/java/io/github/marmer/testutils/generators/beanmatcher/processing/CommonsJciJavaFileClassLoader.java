package io.github.marmer.testutils.generators.beanmatcher.processing;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.MemoryResourceStore;
import org.apache.commons.jci.stores.ResourceStore;
import org.apache.commons.jci.stores.ResourceStoreClassLoader;

import java.nio.file.Path;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * The Class CommonsJciJavaFileClassLoader.
 *
 * @author  MarMer
 * @since   28.06.2017
 */
@CommonsLog
public class CommonsJciJavaFileClassLoader implements JavaFileClassLoader {
	private static final String SOURCE_ENCODING = "UTF-8";
	private static final String JAVA_VERSION = "1.7";

	private final JavaCompiler compiler = new JavaCompilerFactory().createCompiler("eclipse");
	private final JavaCompilerSettings compilerSettings = compiler.createDefaultSettings();
	private final Path sourceBaseDir;
	private ClassLoader classLoader;

	/**
	 * Instantiates a new commons jci java file class loader.
	 *
	 * @param  sourceBaseDir  the source base dir
	 * @param  classLoader    the class loader
	 */
	public CommonsJciJavaFileClassLoader(final Path sourceBaseDir, final ClassLoader classLoader) {
		this.sourceBaseDir = sourceBaseDir;
		this.classLoader = classLoader;

		compilerSettings.setSourceEncoding(SOURCE_ENCODING);
		compilerSettings.setSourceVersion(JAVA_VERSION);
		compilerSettings.setTargetVersion(JAVA_VERSION);
	}

	/* (non-Javadoc)
	 * @see io.github.marmer.testutils.generators.beanmatcher.processing.JavaFileClassLoader#load(java.util.List)
	 */
	@Override
	public List<Class<?>> load(final List<Path> sourceFiles) {
		final FileResourceReader sourceFolderResource = new FileResourceReader(sourceBaseDir.toFile());

		final MemoryResourceStore pStore = new MemoryResourceStore();
		compiler.compile(resourcePathOf(sourceFiles), sourceFolderResource, pStore, classLoader,
			compilerSettings);

		return loadClassesFor(sourceFiles, pStore);
	}

	private List<Class<?>> loadClassesFor(final List<Path> sourceFiles, final MemoryResourceStore pStore) {
		return sourceFiles.stream().map(sourceBaseDir::relativize).map(Path::toString)
			.map(this::toQualifiedClassName).map(qualifiedName -> loadClass(qualifiedName, pStore)).filter(
				Objects::nonNull).collect(Collectors
				.toList());
	}

	private String[] resourcePathOf(final List<Path> sourceFiles) {
		return sourceFiles.stream().map(sourceBaseDir::relativize).map(Path::toString).map(this::toUnixPath).toArray(
				size -> new String[size]);
	}

	private String toUnixPath(final String anyPathString) {
		return anyPathString.replace("\\", "/");
	}

	private String toQualifiedClassName(final String anyPathString) {
		return anyPathString.replaceAll("\\\\|/", ".").replace(".java", "");
	}

	private Class<?> loadClass(final String qualifiedClassName, final MemoryResourceStore pStore) {
		try {
			final ClassLoader classLoader = new ResourceStoreClassLoader(this.classLoader,
					new ResourceStore[] {
						pStore
					});
			return classLoader.loadClass(qualifiedClassName);
		} catch (ClassNotFoundException e) {

			log.error("No Class exists with name: " + qualifiedClassName, e);
			return null;
		}
	}

}
