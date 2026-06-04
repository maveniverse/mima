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
import java.util.*;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.*;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

public class MemoizingRepositorySystemSupplierLookup implements Lookup {
    private final RepositorySystemSupplier repositorySystemSupplier;

    public MemoizingRepositorySystemSupplierLookup() {
        this.repositorySystemSupplier = new RepositorySystemSupplier();
    }

    public RepositorySystem get() {
        return repositorySystemSupplier.get();
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
            Method method = RepositorySystemSupplier.class.getMethod(methodName);
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
}
