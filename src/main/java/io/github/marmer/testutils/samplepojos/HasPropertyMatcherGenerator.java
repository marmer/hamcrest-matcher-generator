package io.github.marmer.testutils.samplepojos;

import java.io.IOException;
import java.nio.file.Path;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class HasPropertyMatcherGenerator {

	private static final String POSTFIX = "Matcher";

	public void generateMatcherFor(final Class<?> type, final Path outputDir) throws IOException {
		CodeBlock codeBlock = CodeBlock.builder().add("System.out.println(\"mops\");").build();
		MethodSpec isType = MethodSpec.methodBuilder("isSimplePojo").returns(TypeName.VOID).addCode(codeBlock).build();
		TypeSpec typeSpec = TypeSpec.classBuilder(type.getSimpleName() + POSTFIX).addModifiers(Modifier.PUBLIC)
				.addMethod(isType).build();
		JavaFile javaFile = JavaFile.builder(type.getPackage().getName(), typeSpec).indent("\t")
				.skipJavaLangImports(true).build();
		javaFile.writeTo(outputDir);
	}

}
