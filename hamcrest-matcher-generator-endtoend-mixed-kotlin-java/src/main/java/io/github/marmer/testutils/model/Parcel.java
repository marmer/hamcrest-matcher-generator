/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model;

/** A Java bean with nested Java beans, asserted from Kotlin through the DSL. */
public class Parcel {

    private final long weight;
    private final Recipient recipient;

    public Parcel(final long weight, final Recipient recipient) {
        this.weight = weight;
        this.recipient = recipient;
    }

    public long getWeight() {
        return weight;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public static class Recipient {

        private final String name;
        private final Place place;

        public Recipient(final String name, final Place place) {
            this.name = name;
            this.place = place;
        }

        public String getName() {
            return name;
        }

        public Place getPlace() {
            return place;
        }
    }

    public static class Place {

        private final String city;

        public Place(final String city) {
            this.city = city;
        }

        public String getCity() {
            return city;
        }
    }
}
