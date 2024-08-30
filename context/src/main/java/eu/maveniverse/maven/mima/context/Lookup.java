/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context;

import java.util.Optional;

/**
 * A simple "lookup" that allows to lookup various components. Lookup shares lifecycle with {@link Context}.
 * <p>
 * Note: this component offers access to Resolver internals, but it is up to caller to know really how to use
 * this feature (for example due compatibility reasons). Ideally, you do not want to use this, or use it only
 * in some "advanced scenarios".
 *
 * @since 2.4.10
 */
public interface Lookup {
    /**
     * Performs lookup for component with passed in type, and returns it as optional, never {@code null}.
     */
    <T> Optional<T> lookup(Class<T> type);

    /**
     * Performs lookup for component with passed in type and name, and returns it as optional, never {@code null}.
     */
    <T> Optional<T> lookup(Class<T> type, String name);
}
