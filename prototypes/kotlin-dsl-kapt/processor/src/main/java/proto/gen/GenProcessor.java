package proto.gen;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * For every @Gen class Foo: writes FooMatcher.java via the Filer and, only when
 * running under KAPT (option kapt.kotlin.generated present), FooDsl.kt into that dir.
 */
public class GenProcessor extends AbstractProcessor {
    static final String KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated";

    @Override public Set<String> getSupportedAnnotationTypes() { return Set.of(Gen.class.getName()); }
    @Override public Set<String> getSupportedOptions() { return Set.of(KAPT_KOTLIN_GENERATED); }
    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        String kotlinDir = processingEnv.getOptions().get(KAPT_KOTLIN_GENERATED);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "GenProcessor: " + KAPT_KOTLIN_GENERATED + " = " + kotlinDir);
        for (Element e : roundEnv.getElementsAnnotatedWith(Gen.class)) {
            TypeElement type = (TypeElement) e;
            String pkg = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
            String simple = type.getSimpleName().toString();
            try {
                writeJavaMatcher(type, pkg, simple, kotlinDir);
                if (kotlinDir != null) {
                    writeKotlinDsl(Path.of(kotlinDir), pkg, simple);
                }
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.toString(), type);
            }
        }
        return true;
    }

    private void writeJavaMatcher(TypeElement type, String pkg, String simple, String kotlinDir) throws IOException {
        String lit = kotlinDir == null ? "null" : "\"" + kotlinDir.replace("\\", "\\\\") + "\"";
        try (Writer w = processingEnv.getFiler().createSourceFile(pkg + "." + simple + "Matcher", type).openWriter()) {
            w.write("package " + pkg + ";\n\n"
                    + "public class " + simple + "Matcher {\n"
                    + "    public static final String KAPT_KOTLIN_GENERATED = " + lit + ";\n"
                    + "    private final StringBuilder checks = new StringBuilder();\n"
                    + "    public static " + simple + "Matcher is" + simple + "() { return new " + simple + "Matcher(); }\n"
                    + "    public " + simple + "Matcher with(String property, Object value) {\n"
                    + "        checks.append(property).append('=').append(value).append(';');\n"
                    + "        return this;\n"
                    + "    }\n"
                    + "    public boolean matches(" + type.getQualifiedName() + " item) { return item != null; }\n"
                    + "    public String describe() { return \"" + simple + "[\" + checks + \"]\"; }\n"
                    + "}\n");
        }
    }

    private void writeKotlinDsl(Path dir, String pkg, String simple) throws IOException {
        Path target = dir.resolve(pkg.replace('.', '/')).resolve(simple + "Dsl.kt");
        Files.createDirectories(target.getParent());
        String fn = Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
        Files.writeString(target, "package " + pkg + "\n\n"
                + "/** Kotlin DSL entry point; calls into the Java-generated " + simple + "Matcher. */\n"
                + "fun " + fn + "(block: " + simple + "Matcher.() -> Unit): " + simple + "Matcher =\n"
                + "    " + simple + "Matcher.is" + simple + "().apply(block)\n", StandardCharsets.UTF_8);
    }
}
