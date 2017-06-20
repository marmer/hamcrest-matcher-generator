package io.github.marmer.testutils.utils.matchers;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.FileResourceStore;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;


public abstract class GeneratedFileCompiler {
	private static final String JAVA_FILE_POSTFIX = ".java";
	private static final String SOURCE_ENCODING = "UTF-8";
	private static final String JAVA_VERSION = "1.7";

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

	private String getGeneratedRelativePathOfUnixString(final Class<?> type) {
		return getGeneratedRelativePathOf(type).toString().replaceAll("\\\\", "/");
	}

	private Path getGeneratedRelativePathOf(final Class<?> type) {
		return getPackagePath(type).resolve(getJavaFileNameFor(type));
	}

	private Path getPackagePath(final Class<?> type) {
		return Paths.get(getPackageNameOf(type).replaceAll("\\.", "/"));
	}

	private String getPackageNameOf(final Class<?> type) {
		return type.getPackage().getName();
	}

	private String getJavaFileNameFor(final Class<?> type) {
		return getGeneratedClassNameFor(type) + JAVA_FILE_POSTFIX;
	}

	public String getGeneratedFullQualifiedClassNameFor(final Class<?> type) {
		return getPackageNameOf(type) + "." + getGeneratedClassNameFor(type);
	}

	public Path getGeneratedSourcePathFor(final Class<?> type) {
		return srcOutputDir.resolve(getGeneratedRelativePathOf(type));
	}

	public abstract String getGeneratedClassNameFor(final Class<?> type);
}
