/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MavenModelReaderTest {
    private static Stream<RemoteRepository> repositories() {
        return Arrays.stream(new RemoteRepository[] {
            null, // context
            ContextOverrides.CENTRAL, // central override
            new RemoteRepository.Builder("foobar", "default", "https://repo1.maven.org/maven2").build() // alt central
        });
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void smoke(RemoteRepository overrideRepository) throws Exception {
        try (Context context =
                Runtimes.INSTANCE.getRuntime().create(ContextOverrides.create().build())) {
            MavenModelReader reader = new MavenModelReader(context);

            ModelResponse response = reader.readModel(ModelRequest.builder()
                    .setArtifact(new DefaultArtifact("org.apache.maven:maven-core:3.9.9"))
                    .setRepositories(overrideRepository != null ? Collections.singletonList(overrideRepository) : null)
                    .setRequestContext("test")
                    .build());
            assertNotNull(response);
            Model model;

            // REPO
            ArtifactRepository responseRepository = response.getRepository();
            if (overrideRepository == null) {
                assertEquals(context.remoteRepositories().get(0), responseRepository);
            } else {
                assertEquals(overrideRepository, responseRepository);
            }

            // RAW
            model = response.getRawModel();
            assertNotNull(model);
            assertNull(model.getGroupId());
            assertEquals("maven-core", model.getArtifactId());
            assertNull(model.getVersion());
            assertNull(model.getUrl());

            // Effective
            model = response.getEffectiveModel();
            assertNotNull(model);
            assertEquals("org.apache.maven", model.getGroupId());
            assertEquals("maven-core", model.getArtifactId());
            assertEquals("3.9.9", model.getVersion());
            assertEquals("https://maven.apache.org/ref/3.9.9/maven-core/", model.getUrl());

            ArtifactDescriptorResult result;
            Artifact artifact;
            // ADR out of RAW
            result = response.toArtifactDescriptorResult(response.interpolateModel(response.getRawModel()));
            // we cannot compare this RESOLVED artifact (has file and properties)
            artifact = result.getArtifact();
            assertEquals(
                    new DefaultArtifact(
                            artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getExtension(),
                            artifact.getVersion()),
                    new DefaultArtifact("org.apache.maven:maven-core:3.9.9"));
            assertEquals(28, result.getDependencies().size());
            assertEquals(0, result.getManagedDependencies().size());

            // ADR out of EFFECTIVE
            result = response.toArtifactDescriptorResult(response.getEffectiveModel());
            // we cannot compare this RESOLVED artifact (has file and properties)
            artifact = result.getArtifact();
            assertEquals(
                    new DefaultArtifact(
                            artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getExtension(),
                            artifact.getVersion()),
                    new DefaultArtifact("org.apache.maven:maven-core:3.9.9"));
            assertEquals(30, result.getDependencies().size());
            assertEquals(74, result.getManagedDependencies().size());
        }
    }
}
