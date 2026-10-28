/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.time.LocalDateTime;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.junit.jupiter.api.Test;

class MatcherGeneratorTest {

    @Test
    void aPropertyWithoutNameStillGetsItsMethodsWithoutAName() {
        final TypeMirror string = TypeMirrors.stringType();
        final TypeModel model =
                new TypeModel(
                        "some.Pojo",
                        "Pojo",
                        "some",
                        "some",
                        List.of(),
                        TypeModel.Kind.POJO,
                        string,
                        List.of(new PropertyModel("", string, "get", Nullability.UNSPECIFIED)),
                        List.of());

        final List<String> methodNames =
                new MatcherGenerator(
                                model,
                                new GenerationContext(
                                        "generator", LocalDateTime.of(2020, 1, 1, 0, 0), false))
                        .getPreparedTypeSpecBuilder().build().methodSpecs.stream()
                                .map(method -> method.name)
                                .toList();

        assertTrue(methodNames.contains("with"), () -> "Methods: " + methodNames);
        assertTrue(methodNames.contains("reset"), () -> "Methods: " + methodNames);
    }
}
