/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Layout of Maven User Home, by default {@code $HOME/.m2}.
 */
public final class MavenUserHomeImpl implements MavenUserHome {
    private final Path mavenUserHome;

    private final Path settingsXmlOverride;

    private final Path settingsSecurityXmlOverride;

    private final Path toolchainsXmlOverride;

    private final Path localRepositoryOverride;

    public MavenUserHomeImpl(Path mavenUserHome) {
        this(mavenUserHome, null, null, null, null);
    }

    public MavenUserHomeImpl(
            Path mavenUserHome,
            Path settingsXmlOverride,
            Path settingsSecurityXmlOverride,
            Path toolchainsXmlOverride,
            Path localRepositoryOverride) {
        this.mavenUserHome = requireNonNull(mavenUserHome);
        this.settingsXmlOverride = settingsXmlOverride;
        this.settingsSecurityXmlOverride = settingsSecurityXmlOverride;
        this.toolchainsXmlOverride = toolchainsXmlOverride;
        this.localRepositoryOverride = localRepositoryOverride;
    }

    public MavenUserHomeImpl withLocalRepository(Path localRepository) {
        return new MavenUserHomeImpl(
                mavenUserHome,
                settingsXmlOverride,
                settingsSecurityXmlOverride,
                toolchainsXmlOverride,
                localRepository);
    }

    @Override
    public MavenUserHomeImpl derive(ContextOverrides contextOverrides) {
        return new MavenUserHomeImpl(
                contextOverrides.getMavenUserHomeOverride() != null
                        ? contextOverrides.getMavenUserHomeOverride()
                        : mavenUserHome,
                contextOverrides.getUserSettingsXmlOverride() != null
                        ? contextOverrides.getUserSettingsXmlOverride()
                        : settingsXmlOverride,
                contextOverrides.getUserSettingsSecurityXmlOverride() != null
                        ? contextOverrides.getUserSettingsSecurityXmlOverride()
                        : settingsSecurityXmlOverride,
                contextOverrides.getUserToolchainsXmlOverride() != null
                        ? contextOverrides.getUserToolchainsXmlOverride()
                        : toolchainsXmlOverride,
                contextOverrides.getLocalRepositoryOverride() != null
                        ? contextOverrides.getLocalRepositoryOverride()
                        : localRepositoryOverride);
    }

    @Override
    public Path basedir() {
        return mavenUserHome;
    }

    @Override
    public Path settingsXml() {
        if (settingsXmlOverride != null) {
            return settingsXmlOverride;
        }
        return basedir().resolve("settings.xml");
    }

    @Override
    public Path settingsSecurityXml() {
        if (settingsSecurityXmlOverride != null) {
            return settingsSecurityXmlOverride;
        }
        return basedir().resolve("settings-security.xml");
    }

    @Override
    public Path toolchainsXml() {
        if (toolchainsXmlOverride != null) {
            return toolchainsXmlOverride;
        }
        return basedir().resolve("toolchains.xml");
    }

    @Override
    public Path localRepository() {
        if (localRepositoryOverride != null) {
            return localRepositoryOverride;
        }
        return basedir().resolve("repository");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MavenUserHomeImpl that = (MavenUserHomeImpl) o;
        return Objects.equals(mavenUserHome, that.mavenUserHome)
                && Objects.equals(settingsXmlOverride, that.settingsXmlOverride)
                && Objects.equals(settingsSecurityXmlOverride, that.settingsSecurityXmlOverride)
                && Objects.equals(toolchainsXmlOverride, that.toolchainsXmlOverride)
                && Objects.equals(localRepositoryOverride, that.localRepositoryOverride);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mavenUserHome,
                settingsXmlOverride,
                settingsSecurityXmlOverride,
                toolchainsXmlOverride,
                localRepositoryOverride);
    }
}
