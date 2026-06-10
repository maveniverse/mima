/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonesisu;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.internal.MavenUserHomeImpl;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.junit.jupiter.api.Test;

@Named
public class StandaloneSisuRuntimeTest extends AbstractModule {
    @Inject
    private Runtime runtime;

    @Override
    protected void configure() {
        try {
            ContextOverrides overrides = ContextOverrides.create().build();
            Path baseDir = Paths.get("target/basedir");
            Files.createDirectories(baseDir);
            bind(PreBoot.class).toInstance(new PreBoot(overrides, new MavenUserHomeImpl(baseDir), null, baseDir));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void smoke() {
        ClassLoader classloader = StandaloneSisuRuntime.class.getClassLoader();
        Guice.createInjector(new WireModule(new SpaceModule(new URLClassSpace(classloader), BeanScanning.ON, true)))
                .injectMembers(this);
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            VersionResult versionResult = context.repositorySystem()
                    .resolveVersion(
                            context.repositorySystemSession(),
                            new VersionRequest(
                                    new DefaultArtifact("eu.maveniverse.maven.mima:context:LATEST"),
                                    context.remoteRepositories(),
                                    "test"));
            // dynamic version should be resolved to some static version
            assertNotEquals("LATEST", versionResult.getVersion());
        } catch (VersionResolutionException e) {
            throw new RuntimeException(e);
        }
    }
}
