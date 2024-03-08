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
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Layout of Maven System Home, usually set with {@code $MAVEN_HOME} environment variable, or {@code maven.home}
 * Java System Property (by Maven itself).
 */
public final class MavenSystemHomeImpl implements MavenSystemHome {
    private final Path mavenSystemHome;

    private final Path settingsXmlOverride;

    private final Path toolchainsXmlOverride;

    public MavenSystemHomeImpl(Path mavenSystemHome) {
        this(mavenSystemHome, null, null);
    }

    public MavenSystemHomeImpl(Path mavenSystemHome, Path settingsXmlOverride, Path toolchainsXmlOverride) {
        this.mavenSystemHome = requireNonNull(mavenSystemHome);
        this.settingsXmlOverride = settingsXmlOverride;
        this.toolchainsXmlOverride = toolchainsXmlOverride;
    }

    @Override
    public MavenSystemHomeImpl derive(ContextOverrides contextOverrides) {
        return new MavenSystemHomeImpl(
                contextOverrides.getMavenSystemHomeOverride() != null
                        ? contextOverrides.getMavenSystemHomeOverride()
                        : mavenSystemHome,
                contextOverrides.getGlobalSettingsXmlOverride() != null
                        ? contextOverrides.getGlobalSettingsXmlOverride()
                        : settingsXmlOverride,
                contextOverrides.getGlobalToolchainsXmlOverride() != null
                        ? contextOverrides.getGlobalToolchainsXmlOverride()
                        : toolchainsXmlOverride);
    }

    @Override
    public Path basedir() {
        return mavenSystemHome;
    }

    @Override
    public Path bin() {
        return basedir().resolve("bin");
    }

    @Override
    public Path boot() {
        return basedir().resolve("boot");
    }

    @Override
    public Path conf() {
        return basedir().resolve("conf");
    }

    @Override
    public Path lib() {
        return basedir().resolve("lib");
    }

    public Path libExt() {
        return lib().resolve("ext");
    }

    @Override
    public Path settingsXml() {
        if (settingsXmlOverride != null) {
            return settingsXmlOverride;
        }
        return conf().resolve("settings.xml");
    }

    @Override
    public Path toolchainsXml() {
        if (toolchainsXmlOverride != null) {
            return toolchainsXmlOverride;
        }
        return conf().resolve("toolchains.xml");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MavenSystemHomeImpl that = (MavenSystemHomeImpl) o;
        return Objects.equals(mavenSystemHome, that.mavenSystemHome)
                && Objects.equals(settingsXmlOverride, that.settingsXmlOverride)
                && Objects.equals(toolchainsXmlOverride, that.toolchainsXmlOverride);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mavenSystemHome, settingsXmlOverride, toolchainsXmlOverride);
    }
}
