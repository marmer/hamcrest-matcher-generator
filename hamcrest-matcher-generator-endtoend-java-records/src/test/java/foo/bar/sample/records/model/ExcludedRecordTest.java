package foo.bar.sample.records.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcludedRecordTest {

    @Test
    @DisplayName("A record excluded from its configured package gets no matcher, its package siblings do")
    void anExcludedRecordGetsNoMatcher() {
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName("foo.bar.sample.records.model.HiddenRecordMatcher"));
        assertDoesNotThrow(() -> Class.forName("foo.bar.sample.records.model.PersonRecordMatcher"));
    }
}
