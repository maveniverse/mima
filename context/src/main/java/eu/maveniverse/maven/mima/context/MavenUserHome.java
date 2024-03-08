/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context;

import java.nio.file.Path;

/**
 * Interface pointing to Maven User Home and various locations of interests within it.
 *
 * @since 2.4.0
 */
public interface MavenUserHome {
    Path basedir();

    Path settingsXml();

    Path settingsSecurityXml();

    Path toolchainsXml();

    Path localRepository();

    /**
     * Derives new maven user home from itself with overrides applied.
     *
     * @since 2.4.4
     */
    MavenUserHome derive(ContextOverrides contextOverrides);
}
