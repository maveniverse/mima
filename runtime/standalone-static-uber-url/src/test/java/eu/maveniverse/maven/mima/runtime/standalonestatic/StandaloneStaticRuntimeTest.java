/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.junit.jupiter.api.Test;

public class StandaloneStaticRuntimeTest {
    @Test
    void smoke() {
        StandaloneStaticRuntime runtime = new StandaloneStaticRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withLocalRepositoryOverride(Paths.get("target/local-repo"))
                .build())) {
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

    @Test
    void smokeWithExtensions() {
        TrustedChecksumsSource fakeSource = new TrustedChecksumsSource() {
            @Override
            public Map<String, String> getTrustedArtifactChecksums(
                    RepositorySystemSession session,
                    Artifact artifact,
                    ArtifactRepository artifactRepository,
                    List<ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
                return Collections.singletonMap("SHA-1", "fake");
            }

            @Override
            public Writer getTrustedArtifactChecksumsWriter(RepositorySystemSession session) {
                return null;
            }
        };
        // set up static extensions
        HashMap<String, Object> trustedSources = new HashMap<>();
        trustedSources.put("fake", fakeSource);
        HashMap<Class<?>, Map<String, Object>> extensions = new HashMap<>();
        extensions.put(TrustedChecksumsSource.class, trustedSources);
        // set up configuration
        HashMap<String, String> userProperties = new HashMap<>();
        userProperties.put("aether.artifactResolver.postProcessor.trustedChecksums", "true");
        userProperties.put("aether.artifactResolver.postProcessor.trustedChecksums.checksumAlgorithms", "SHA-1");

        StandaloneStaticRuntime runtime = new StandaloneStaticRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withLocalRepositoryOverride(Paths.get("target/local-repo"))
                .withStaticExtensions(extensions)
                .userProperties(userProperties)
                .build())) {
            context.repositorySystem()
                    .resolveArtifact(
                            context.repositorySystemSession(),
                            new ArtifactRequest(
                                    new DefaultArtifact("eu.maveniverse.maven.mima:context:2.4.45"),
                                    context.remoteRepositories(),
                                    "test"));
            fail("Resolution should have fail");
        } catch (ArtifactResolutionException e) {
            assertTrue(
                    e.getMessage().contains("Checksum validation failed, expected 'fake' (PROVIDED)"), e.getMessage());
        }
    }

    @Test
    void smokeWithBadExtensions() {
        // set up static extensions with bad type (of type String, but key is TrustedChecksumsSource)
        HashMap<String, Object> trustedSources = new HashMap<>();
        trustedSources.put("fake", "this should not be a string but a TrustedChecksumsSource instance");
        HashMap<Class<?>, Map<String, Object>> extensions = new HashMap<>();
        extensions.put(TrustedChecksumsSource.class, trustedSources);

        StandaloneStaticRuntime runtime = new StandaloneStaticRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withLocalRepositoryOverride(Paths.get("target/local-repo"))
                .withStaticExtensions(extensions)
                .build())) {
            fail("Context creation should have fail");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "User provided static extensions for key org.eclipse.aether.spi.checksums.TrustedChecksumsSource are of wrong type"),
                    e.getMessage());
        }
    }
}
