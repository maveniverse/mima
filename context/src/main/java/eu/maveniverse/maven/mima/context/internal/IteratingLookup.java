/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Lookup;
import java.util.*;

/**
 * A {@link Lookup} implementation that is able to iterate through several lookups, applying "first deliver wins"
 * strategy.
 *
 * @since 2.4.10
 */
public final class IteratingLookup implements Lookup {
    private final Collection<Lookup> lookups;

    public IteratingLookup(Lookup... lookups) {
        this(Arrays.asList(lookups));
    }

    public IteratingLookup(Collection<Lookup> lookups) {
        this.lookups = requireNonNull(lookups);
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        for (Lookup lookup : lookups) {
            Optional<T> result = lookup.lookup(type);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        for (Lookup lookup : lookups) {
            Optional<T> result = lookup.lookup(type, name);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
