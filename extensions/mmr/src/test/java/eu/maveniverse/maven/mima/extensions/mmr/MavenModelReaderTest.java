/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtimes;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.junit.jupiter.api.Test;

public class MavenModelReaderTest {
    @Test
    void smoke() throws Exception {
        try (Context context =
                Runtimes.INSTANCE.getRuntime().create(ContextOverrides.create().build())) {
            MavenModelReader reader = new MavenModelReader(context);

            ModelResponse response = reader.readModel(new ArtifactDescriptorRequest(
                    new DefaultArtifact("org.apache.maven:maven-core:3.9.9"), context.remoteRepositories(), "test"));
            assertNotNull(response);
            assertNotNull(response.toModel(ModelLevel.EFFECTIVE));
        }
    }
}
