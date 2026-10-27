/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model;

public class SomePojo {

    private final String someProp;

    public SomePojo(final String someProp) {
        this.someProp = someProp;
    }

    public String getSomeProp() {
        return someProp;
    }
}
