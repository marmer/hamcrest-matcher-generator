package io.github.marmer.testutils.generators.beanmatcher;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.MemoryResourceStore;
import org.apache.commons.jci.stores.ResourceStore;
import org.apache.commons.jci.stores.ResourceStoreClassLoader;

import java.nio.file.Path;

import java.util.List;
import java.util.stream.Collectors;


@CommonsLog
public class CommonsJciJavaFileClassLoader implements JavaFileClassLoader {
	private static final String SOURCE_ENCODING = "UTF-8";
	private static final String JAVA_VERSION = "1.7";

	private final JavaCompiler compiler = new JavaCompilerFactory().createCompiler("eclipse");
	private final JavaCompilerSettings compilerSettings = compiler.createDefaultSettings();
	private final Path sourceBaseDir;

	public CommonsJciJavaFileClassLoader(final Path sourceBaseDir) {
		this.sourceBaseDir = sourceBaseDir;

		compilerSettings.setSourceEncoding(SOURCE_ENCODING);
		compilerSettings.setSourceVersion(JAVA_VERSION);
		compilerSettings.setTargetVersion(JAVA_VERSION);
	}

	@Override
	public List<Class<?>> load(final List<Path> sourceFiles) {
		final FileResourceReader sourceFolderResource = new FileResourceReader(sourceBaseDir.toFile());

		final MemoryResourceStore pStore = new MemoryResourceStore();
		final CompilationResult compileResult = compiler.compile(resourcePathOf(sourceFiles), sourceFolderResource,
				pStore,
				getClass().getClassLoader(),
				compilerSettings);

		return loadClassesFor(sourceFiles, pStore);
	}

	private List<Class<?>> loadClassesFor(final List<Path> sourceFiles, final MemoryResourceStore pStore) {
		return sourceFiles.stream().map(sourceBaseDir::relativize).map(Path::toString)
			.map(this::toQualifiedClassName).map(qualifiedName -> loadClass(qualifiedName, pStore)).collect(Collectors
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
			final ClassLoader classLoader = new ResourceStoreClassLoader(getClass().getClassLoader(),
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
