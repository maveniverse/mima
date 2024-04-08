/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonestatic;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Lookup;
import eu.maveniverse.maven.mima.context.internal.IteratingLookup;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import java.util.NoSuchElementException;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;

public class StandaloneStaticRuntime extends StandaloneRuntimeSupport {

    public StandaloneStaticRuntime() {
        this("standalone-static", 40);
    }

    public StandaloneStaticRuntime(String name, int priority) {
        super(name, priority);
    }

    @Override
    public boolean managedRepositorySystem() {
        return true;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        PreBoot preBoot = preBoot(overrides);
        Lookup lookup = new IteratingLookup(createStaticLookup(preBoot), createRepositorySystemLookup(preBoot));
        RepositorySystem repositorySystem = lookup.lookup(RepositorySystem.class)
                .orElseThrow(() -> new NoSuchElementException("No RepositorySystem present"));
        SettingsBuilder settingsBuilder = lookup.lookup(SettingsBuilder.class)
                .orElseThrow(() -> new NoSuchElementException("No SettingsBuilder present"));
        SettingsDecrypter settingsDecrypter = lookup.lookup(SettingsDecrypter.class)
                .orElseThrow(() -> new NoSuchElementException("No SettingsDecrypter present"));
        ProfileSelector profileSelector = lookup.lookup(ProfileSelector.class)
                .orElseThrow(() -> new NoSuchElementException("No ProfileSelector present"));
        return buildContext(
                this,
                preBoot,
                repositorySystem,
                settingsBuilder,
                settingsDecrypter,
                profileSelector,
                lookup,
                repositorySystem::shutdown);
    }

    protected Lookup createStaticLookup(PreBoot preBoot) {
        return new StaticLookup(preBoot);
    }

    protected Lookup createRepositorySystemLookup(PreBoot preBoot) {
        return new MemoizingRepositorySystemSupplierLookup();
    }
}
