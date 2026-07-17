/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonestatic;

import eu.maveniverse.maven.mima.context.Lookup;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;

public class MemoizingRepositorySystemSupplierLookup implements Lookup {
    private final MimaRepositorySystemSupplier repositorySystemSupplier;

    public MemoizingRepositorySystemSupplierLookup(Map<Class<?>, Map<String, Object>> staticExtensions) {
        this.repositorySystemSupplier = new MimaRepositorySystemSupplier(staticExtensions);
    }

    public RepositorySystem get() {
        return lookup(RepositorySystem.class).orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        return lookup(type, "default");
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        return Optional.ofNullable(lookupMap(type).get(name));
    }

    @SuppressWarnings({"unchecked"})
    private <T> Map<String, T> lookupMap(Class<T> type) {
        String methodName = "get" + type.getSimpleName();
        try {
            Method method = MimaRepositorySystemSupplier.class.getMethod(methodName);
            Object result = method.invoke(repositorySystemSupplier);
            if (result instanceof Map) {
                return (Map<String, T>) result;
            }
            return Collections.singletonMap("default", (T) result);
        } catch (NoSuchMethodException e) {
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class MimaRepositorySystemSupplier extends RepositorySystemSupplier {
        private final Map<Class<?>, Map<String, Object>> staticExtensions;

        private MimaRepositorySystemSupplier(Map<Class<?>, Map<String, Object>> staticExtensions) {
            this.staticExtensions = staticExtensions;

            // validate: key class should be assignable from the value map values
            // (as they should be implementing key class, that is usually interface)
            for (Class<?> key : staticExtensions.keySet()) {
                Map<String, Object> values = staticExtensions.get(key);
                for (Object value : values.values()) {
                    if (!key.isInstance(value)) {
                        throw new IllegalArgumentException(String.format(
                                "User provided static extensions for key %s are of wrong type", key.getName()));
                    }
                }
            }
        }

        private static boolean isPresent(String clazzName) {
            try {
                MemoizingRepositorySystemSupplierLookup.class.getClassLoader().loadClass(clazzName);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        @Override
        protected Map<String, TransporterFactory> createTransporterFactories() {
            HashMap<String, TransporterFactory> result = new HashMap<>();
            result.put(FileTransporterFactory.NAME, new FileTransporterFactory());
            if (isPresent("org.eclipse.aether.transport.url.UrlTransporterFactory")) {
                result.put(
                        org.eclipse.aether.transport.url.UrlTransporterFactory.NAME,
                        new org.eclipse.aether.transport.url.UrlTransporterFactory(
                                getChecksumExtractor(), getPathProcessor()));
            }
            if (isPresent("org.eclipse.aether.transport.apache.ApacheTransporterFactory")) {
                result.put(
                        org.eclipse.aether.transport.apache.ApacheTransporterFactory.NAME,
                        new org.eclipse.aether.transport.apache.ApacheTransporterFactory(
                                getChecksumExtractor(), getPathProcessor()));
            }
            return result;
        }

        @Override
        protected Map<String, TrustedChecksumsSource> createTrustedChecksumsSources() {
            Map<String, TrustedChecksumsSource> result = super.createTrustedChecksumsSources();
            Map<String, Object> userProvided = staticExtensions.get(TrustedChecksumsSource.class);
            if (userProvided != null) {
                for (Map.Entry<String, Object> entry : userProvided.entrySet()) {
                    result.put(entry.getKey(), (TrustedChecksumsSource) entry.getValue());
                }
            }
            return result;
        }
    }
}
