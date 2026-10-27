package io.github.marmer.testutils.lombok.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Covers every getter shape Lombok generates: plain, boxed boolean, primitive boolean and primitive boolean fields
 * that already start with "is".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LombokPojo {
    @Nullable
    private String name;
    private int count;
    private boolean active;
    @Nullable
    private Boolean flagged;
    private boolean isEnabled;
    private List<String> tags = List.of();
}
