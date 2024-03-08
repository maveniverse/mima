/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonesisu;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import eu.maveniverse.maven.mima.runtime.standalonesisu.internal.SisuBooter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public final class StandaloneSisuRuntime extends StandaloneRuntimeSupport {

    private final RepositorySystem repositorySystem;

    private final SettingsBuilder settingsBuilder;

    private final SettingsDecrypter settingsDecrypter;

    private final ProfileSelector profileSelector;

    public StandaloneSisuRuntime() {
        this(null, null, null, null);
    }

    @Inject
    public StandaloneSisuRuntime(
            RepositorySystem repositorySystem,
            SettingsBuilder settingsBuilder,
            SettingsDecrypter settingsDecrypter,
            ProfileSelector profileSelector) {
        super("standalone-sisu", 30);
        this.repositorySystem = repositorySystem;
        this.settingsBuilder = settingsBuilder;
        this.settingsDecrypter = settingsDecrypter;
        this.profileSelector = profileSelector;
    }

    @Override
    public boolean managedRepositorySystem() {
        return repositorySystem == null;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        PreBoot preBoot = preBoot(overrides);
        // managed or unmanaged context: depending on how we booted
        if (repositorySystem == null) {
            SisuBooter booter = SisuBooter.newSisuBooter(preBoot);
            return buildContext(
                    this,
                    preBoot,
                    booter.repositorySystem,
                    booter.settingsBuilder,
                    booter.settingsDecrypter,
                    booter.profileSelector,
                    booter::close);
        } else {
            return buildContext(
                    this, preBoot, repositorySystem, settingsBuilder, settingsDecrypter, profileSelector, null);
        }
    }
}
