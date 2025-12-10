/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context;

/**
 * Runtime is a factory for {@link Context} instances.
 */
public interface Runtime {
    /**
     * String representation returned for versions, when discovery was unsuccessful.
     *
     * @since 2.4.38
     */
    String UNKNOWN_VERSION = "(unknown)";

    /**
     * The runtime name (mostly for keying purposes), never {@code null}.
     */
    String name();

    /**
     * The runtime version, never {@code null}.
     */
    String version();

    /**
     * The priority of runtime instance. Priorities use natural integer ordering.
     */
    int priority();

    /**
     * Returns a string representing Maven version this runtime uses, never {@code null}. This mostly stands for
     * "maven models" version, except when MIMA runs inside of Maven, when it carries the "actual Maven version".
     */
    String mavenVersion();

    /**
     * Returns a string representing Resolver version this runtime uses, never {@code null}. In case of embedded
     * Maven, discovery of resolver is not possible due Maven classloader encapsulation (then resolver version
     * can be derived from Maven version).
     *
     * @since 2.4.38
     * @return discovered resolver version or {@link #UNKNOWN_VERSION}
     */
    String resolverVersion();

    /**
     * Returns {@code true} if this runtime creates managed repository system, that is opposite when MIMA runs
     * in Maven (or any other environment providing resolver), where it does not manage it, as hosting Maven or app
     * does. In general, you should always treat "root context" as explained in {@link Context} and your code will be
     * portable.
     */
    boolean managedRepositorySystem();

    /**
     * Creates a {@link Context} instance using passed in {@link ContextOverrides}, never returns {@code null}.
     */
    Context create(ContextOverrides overrides);
}
