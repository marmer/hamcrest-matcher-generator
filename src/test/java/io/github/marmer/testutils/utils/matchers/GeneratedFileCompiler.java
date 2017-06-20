package io.github.marmer.testutils.utils.matchers;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.FileResourceStore;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Path;
import java.nio.file.Paths;


public abstract class GeneratedFileCompiler {
	private static final String JAVA_FILE_POSTFIX = ".java";
	private static final String SOURCE_ENCODING = "UTF-8";
	private static final String JAVA_VERSION = "1.7";

	private static final String FACADE_PACKAGE = "io.github.marmer.testutils";
	private static final String FACADE_NAME = "BeanPropertyMatchers";
	private static final String FULL_QUALIFIED_FACADE_NAME = FACADE_PACKAGE + "." + FACADE_NAME;

	private final Path srcOutputDir;
	private final Path classOutputDir;

	private final JavaCompiler compiler = new JavaCompilerFactory().createCompiler("eclipse");
	private final JavaCompilerSettings compilerSettings = compiler.createDefaultSettings();

	public GeneratedFileCompiler(final Path srcOutputDir, final Path classOutputDir) {
		this.srcOutputDir = srcOutputDir;
		this.classOutputDir = classOutputDir;

		compilerSettings.setSourceEncoding(SOURCE_ENCODING);
		compilerSettings.setSourceVersion(JAVA_VERSION);
		compilerSettings.setTargetVersion(JAVA_VERSION);
	}

	public CompilationResult compileGeneratedSourceFileFor(final Class<?> type) throws IOException {
		final String[] pResourcePaths = {
			getGeneratedRelativePathOfUnixString(type)
		};
		final FileResourceReader sourceFolderResource = new FileResourceReader(srcOutputDir.toFile());
		final FileResourceStore classFolderResource = new FileResourceStore(classOutputDir.toFile());

		return compiler.compile(pResourcePaths, sourceFolderResource, classFolderResource, getClass().getClassLoader(),
				compilerSettings);
	}

	public CompilationResult compileFacade() throws IOException {
		final String[] pResourcePaths = {
			getFacadePath()
		};
		final FileResourceReader sourceFolderResource = new FileResourceReader(srcOutputDir.toFile());
		final FileResourceStore classFolderResource = new FileResourceStore(classOutputDir.toFile());

		return compiler.compile(pResourcePaths, sourceFolderResource, classFolderResource, getClass().getClassLoader(),
				compilerSettings);
	}

	private String getFacadePath() {
		return unixPathStringOf(srcOutputDir.resolve(relativeDirectoryOf(FULL_QUALIFIED_FACADE_NAME)));
	}

	private String getGeneratedRelativePathOfUnixString(final Class<?> type) {
		return unixPathStringOf(generatedRelativePathOf(type));
	}

	private String unixPathStringOf(final Path path) {
		return path.toString().replace("\\", "/");
	}

	private Path generatedRelativePathOf(final Class<?> type) {
		return getPackagePath(type).resolve(getJavaFileNameFor(type));
	}

	private Path getPackagePath(final Class<?> type) {
		return Paths.get(relativeDirectoryOf(packageNameOf(type)));
	}

	private String relativeDirectoryOf(final String packageName) {
		return packageName.replace(".", "/");
	}

	private String packageNameOf(final Class<?> type) {
		return type.getPackage().getName();
	}

	private String getJavaFileNameFor(final Class<?> type) {
		return getGeneratedClassNameFor(type) + JAVA_FILE_POSTFIX;
	}

	public String getGeneratedFullQualifiedClassNameFor(final Class<?> type) {
		return packageNameOf(type) + "." + getGeneratedClassNameFor(type);
	}

	public Path getGeneratedSourcePathFor(final Class<?> type) {
		return srcOutputDir.resolve(generatedRelativePathOf(type));
	}

	public <T> Class<?> loadGeneratedClassFor(final Class<?> type) throws Exception {
		return (Class<?>) getClassLoader().loadClass(getGeneratedFullQualifiedClassNameFor(type));
	}

	private URLClassLoader getClassLoader() throws MalformedURLException {
		final URL url = classOutputDir.toUri().toURL();
		return new URLClassLoader(new URL[] {
					url
				});
	}

	public abstract String getGeneratedClassNameFor(final Class<?> type);

	public Class<?> compileAndLoadGeneratedClassFor(final Class<?> type) throws IOException, Exception {
		compileGeneratedSourceFileFor(type);
		return loadGeneratedClassFor(type);
	}

	public <T> T compileAndLoadInstanceOfGeneratedClassFor(final Class<?> type) throws IOException, Exception,
		InstantiationException, IllegalAccessException {
		compileGeneratedSourceFileFor(type);

		final Class<?> loadClass = loadGeneratedClassFor(type);

		return (T) loadClass.newInstance();
	}

	public Object compileAndLoadFacade() throws Exception {
		compileFacade();
		return getClassLoader().loadClass(FULL_QUALIFIED_FACADE_NAME);
	}

	public Method getFacadeMethodFor(final Class<?> type) throws Exception, NoSuchMethodException {
		final Object facade = compileAndLoadFacade();
		return facade.getClass().getMethod("is" + StringUtils.capitalize(type.getSimpleName()));
	}
}
