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
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.jupiter.api.Test;

public class MavenModelReaderTest {
    @Test
    void smoke() throws Exception {
        try (Context context =
                Runtimes.INSTANCE.getRuntime().create(ContextOverrides.create().build())) {
            MavenModelReader reader = new MavenModelReader(context);

            ModelResponse response = reader.readModel(ModelRequest.builder()
                    .setArtifact(new DefaultArtifact("org.apache.maven:maven-core:3.9.9"))
                    .setRepositories(context.remoteRepositories())
                    .setRequestContext("test")
                    .build());
            assertNotNull(response);
            Model model;

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
