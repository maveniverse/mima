/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonestatic;

import java.util.function.Supplier;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;

/**
 * Override to customize.
 */
public class SettingsBuilderSupplier implements Supplier<SettingsBuilder> {
    @Override
    public SettingsBuilder get() {
        return new DefaultSettingsBuilder(
                new DefaultSettingsReader(), new DefaultSettingsWriter(), new DefaultSettingsValidator());
    }
}
