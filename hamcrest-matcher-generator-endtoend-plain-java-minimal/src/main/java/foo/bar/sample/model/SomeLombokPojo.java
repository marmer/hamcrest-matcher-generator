/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class SomeLombokPojo {
    private String someProp;

    @Getter(AccessLevel.PACKAGE)
    private String someNoProp;
}
