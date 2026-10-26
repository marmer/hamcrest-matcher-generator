package demo;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;

class BarTest {
    @Test
    void plainJavacHasNoKaptOptionAndNoKotlinFile() {
        assertNull(BarMatcher.KAPT_KOTLIN_GENERATED);
        assertEquals("Bar[x=1;]", BarMatcher.isBar().with("x", 1).describe());
        try (var s = Files.walk(Path.of("target"))) {
            assertTrue(s.noneMatch(p -> p.toString().endsWith(".kt")));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
