/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

public enum MavenModelResolverMode {
    /**
     * The "raw model" mode, no inheritance nor proper interpolation applied, POM as is.
     */
    RAW,
    /**
     * The "raw model" mode, but with parent GAV coordinates and interpolation applied, but without inheritance.
     */
    RAW_INTERPOLATED,
    /**
     * The "effective" model with fully built Maven model, without lifecycle plugins.
     */
    EFFECTIVE
}
