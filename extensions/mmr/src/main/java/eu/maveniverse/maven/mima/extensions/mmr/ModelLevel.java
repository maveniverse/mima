/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

/**
 * The processing "levels" of the model.
 */
public enum ModelLevel {
    /**
     * The "raw model" level, as-is in POM file, no inheritance nor proper interpolation applied, just parsed with
     * minimal validation.
     */
    RAW,
    /**
     * RAW + interpolation, based on {@link #RAW}, but with parent GAV coordinates and interpolation applied
     */
    RAW_INTERPOLATED,
    /**
     * The "effective" level with fully built Maven model, without lifecycle plugins.
     */
    EFFECTIVE
}
