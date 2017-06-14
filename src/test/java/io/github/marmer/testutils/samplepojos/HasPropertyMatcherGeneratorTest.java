package io.github.marmer.testutils.samplepojos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.FileResourceStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import lombok.Value;

public class HasPropertyMatcherGeneratorTest {
	private static final String MATCHER_POSTFIX = "Matcher.java";
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
	@InjectMocks
	private HasPropertyMatcherGenerator classUnderTest;
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();
	private Path srcOutputDir;
	private final JavaCompiler compiler = new JavaCompilerFactory().createCompiler("eclipse");
	private final JavaCompilerSettings compilerSettings = compiler.createDefaultSettings();
	private Path classOutputDir;

	@Before
	public void prepareOutputDir() throws Exception {
		this.classOutputDir = temp.newFolder("target").toPath();
	}

	@Before
	public void prepareSourceOutputDir() throws Exception {
		this.srcOutputDir = temp.newFolder("src").toPath();
	}

	@Before
	public void initCompilerSettings() {
		compilerSettings.setSourceEncoding("UTF-8");
		compilerSettings.setSourceVersion("1.7");
		compilerSettings.setTargetVersion("1.7");
	}

	@Test
	public void testGenerateMatcherFor_SimplePojoClassGiven_ShouldCreateJavaFile() throws Exception {
		// Preparation
		classUnderTest.generateMatcherFor(SimplePojo.class, srcOutputDir);

		// Assertion
		assertThat(generatedSourceFileFor(SimplePojo.class), is(anExistingFile()));
	}

	@Test
	public void testGenerateMatcherFor_FileHasBeanCreated_CreatedJavaFileShouldBeCompilableWithoutErrors()
			throws Exception {
		// Preparation
		Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type, srcOutputDir);

		// Assertion

		CompilationResult result = compileGeneratedSourceFileFor(type);
		assertThat("Compile Errors", result.getErrors(), is(emptyArray()));
	}

	@Test
	public void testGenerateMatcherFor_FileHasBeanCreated_CreatedJavaFileShouldBeCompilableWithoutWarnings()
			throws Exception {
		// Preparation
		Class<SimplePojo> type = SimplePojo.class;

		// Execution
		classUnderTest.generateMatcherFor(type, srcOutputDir);

		// Assertion

		CompilationResult result = compileGeneratedSourceFileFor(type);
		assertThat("Compile Warnings", result.getWarnings(), is(emptyArray()));
	}

	private CompilationResult compileGeneratedSourceFileFor(final Class<SimplePojo> type) throws IOException {
		String[] pResourcePaths = { getGeneratedRelativePathOfUnixString(type) };
		FileResourceReader sourceFolderResource = new FileResourceReader(srcOutputDir.toFile());
		FileResourceStore classFolderResource = new FileResourceStore(classOutputDir.toFile());

		return compiler.compile(pResourcePaths, sourceFolderResource, classFolderResource, getClass().getClassLoader(),
				compilerSettings);
	}

	private String getGeneratedRelativePathOfUnixString(final Class<SimplePojo> type) {
		return getGeneratedRelativePathOf(type).toString().replaceAll("\\\\", "/");
	}

	private File generatedSourceFileFor(final Class<?> type) {
		return generatedSourcePathFor(type).toFile();
	}

	private Path generatedSourcePathFor(final Class<?> type) {
		return srcOutputDir.resolve(getGeneratedRelativePathOf(type));
	}

	private Path getGeneratedRelativePathOf(final Class<?> type) {
		return getPackagePath(type).resolve(generatedClassNameFor(type));
	}

	private String generatedClassNameFor(final Class<?> type) {
		return type.getSimpleName() + MATCHER_POSTFIX;
	}

	private Path getPackagePath(final Class<?> type) {
		return Paths.get(getPackageNameOf(type).replaceAll("\\.", "/"));
	}

	private String getPackageNameOf(final Class<?> type) {
		return type.getPackage().getName();
	}

	@Value
	public class SimplePojo {
		private String simpleProp;
	}

}
