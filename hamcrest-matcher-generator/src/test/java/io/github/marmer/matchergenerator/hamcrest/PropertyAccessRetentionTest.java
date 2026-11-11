/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A type registered by a generated matcher must not keep the class loader of the type alive: the
 * registry holds the accessors of the type, and their lambdas hold their class loader.
 */
class PropertyAccessRetentionTest {
    private static final String FIXTURE = RetentionFixture.class.getName();
    private static final String BEAN = RetentionFixture.Bean.class.getName();
    private static final int MAX_COLLECTION_ATTEMPTS = 100;
    private static final int PRESSURE_ARRAYS = 16;
    private static final int PRESSURE_ARRAY_SIZE = 1 << 20;

    @Test
    void testRegisteredType_ThrowAwayClassLoader_ShouldBeCollectedOnceUnreferenced()
            throws Exception {
        final WeakReference<ClassLoader> loader = registerInThrowAwayClassLoader();

        assertTrue(
                collected(loader),
                "the class loader of a registered type is still reachable after GC");
    }

    /**
     * Registers a matcher and its bean in a new class loader. Kept apart from the test, so that no
     * local variable of the test refers to the loader.
     */
    private static WeakReference<ClassLoader> registerInThrowAwayClassLoader() throws Exception {
        final ClassLoader loader =
                new ThrowAwayClassLoader(
                        PropertyAccessRetentionTest.class.getClassLoader(), Set.of(FIXTURE, BEAN));
        loader.loadClass(FIXTURE).getMethod("newMatcher").invoke(null);
        assertNotNull(PropertyAccess.propertiesOfGeneratedType(loader.loadClass(BEAN)));
        return new WeakReference<>(loader);
    }

    private static boolean collected(final WeakReference<ClassLoader> loader)
            throws InterruptedException {
        for (int attempt = 0; attempt < MAX_COLLECTION_ATTEMPTS; attempt++) {
            if (loader.get() == null) {
                return true;
            }
            System.gc();
            allocatePressure();
            Thread.sleep(10);
        }
        return loader.get() == null;
    }

    private static void allocatePressure() {
        final List<byte[]> arrays = new ArrayList<>();
        for (int i = 0; i < PRESSURE_ARRAYS; i++) {
            arrays.add(new byte[PRESSURE_ARRAY_SIZE]);
        }
        arrays.clear();
    }

    /**
     * Defines the named classes itself, from the bytes the test class path holds, and delegates
     * every other class to the parent.
     */
    private static final class ThrowAwayClassLoader extends ClassLoader {
        private final ClassLoader source;
        private final Set<String> ownClasses;

        ThrowAwayClassLoader(final ClassLoader source, final Set<String> ownClasses) {
            super(source);
            this.source = source;
            this.ownClasses = ownClasses;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve)
                throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                final Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }
                if (!ownClasses.contains(name)) {
                    return super.loadClass(name, resolve);
                }
                return define(name);
            }
        }

        private Class<?> define(final String name) throws ClassNotFoundException {
            try (InputStream in = source.getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                final byte[] bytes = in.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (final IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
