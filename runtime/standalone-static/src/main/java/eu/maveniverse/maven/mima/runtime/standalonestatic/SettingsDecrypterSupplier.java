/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.MavenUserHome;
import java.util.HashMap;
import java.util.function.Supplier;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.MavenSecDispatcher;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.LegacyDispatcher;

/**
 * Override to customize.
 */
public class SettingsDecrypterSupplier implements Supplier<SettingsDecrypter> {
    private final MavenUserHome mavenUserHome;
    private final SettingsDecrypter settingsDecrypter;

    public SettingsDecrypterSupplier(MavenUserHome mavenUserHome) {
        this.mavenUserHome = requireNonNull(mavenUserHome);

        HashMap<String, Dispatcher> dispatchers = new HashMap<>();
        dispatchers.put(LegacyDispatcher.NAME, new LegacyDispatcher());
        this.settingsDecrypter = new DefaultSettingsDecrypter(new MavenSecDispatcher(dispatchers));
    }

    @Override
    public SettingsDecrypter get() {
        return settingsDecrypter;
    }
}
