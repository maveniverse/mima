/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;

/**
 * Model Request.
 */
public class ModelRequest {
    private final ModelReaderMode mode;
    private final boolean followRelocation;
    private final ArtifactDescriptorRequest artifactDescriptorRequest;

    private ModelRequest(
            ModelReaderMode mode, boolean followRelocation, ArtifactDescriptorRequest artifactDescriptorRequest) {
        this.mode = requireNonNull(mode);
        this.followRelocation = followRelocation;
        this.artifactDescriptorRequest = requireNonNull(artifactDescriptorRequest);
    }

    public ModelReaderMode getMode() {
        return mode;
    }

    public boolean isFollowRelocation() {
        return followRelocation;
    }

    public Path getPomPath() {
        Artifact artifact = artifactDescriptorRequest.getArtifact();
        if (artifact != null) {
            File pomPath = artifact.getFile();
            if (pomPath != null) {
                return pomPath.toPath();
            }
        }
        return null;
    }

    public ArtifactDescriptorRequest getArtifactDescriptorRequest() {
        return artifactDescriptorRequest;
    }

    public static class Builder {
        private ModelReaderMode mode = ModelReaderMode.EFFECTIVE;
        private Path pomPath;
        private boolean followRelocation = true;
        private ArtifactDescriptorRequest artifactDescriptorRequest = new ArtifactDescriptorRequest();

        public Builder setMode(ModelReaderMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder setFollowRelocation(boolean followRelocation) {
            this.followRelocation = followRelocation;
            return this;
        }

        public Builder setPomPath(Path pomPath) {
            this.pomPath = pomPath;
            return this;
        }

        public Builder setArtifactDescriptorRequest(ArtifactDescriptorRequest artifactDescriptorRequest) {
            this.artifactDescriptorRequest = new ArtifactDescriptorRequest(
                    artifactDescriptorRequest.getArtifact(),
                    artifactDescriptorRequest.getRepositories(),
                    artifactDescriptorRequest.getRequestContext());
            return this;
        }

        public ModelRequest build() {
            if (pomPath != null) {
                Artifact artifact = artifactDescriptorRequest.getArtifact();
                if (artifact == null) {
                    artifact = new DefaultArtifact("irrelevant:irrelevant:irrelevant");
                }
                artifact = artifact.setFile(pomPath.toFile());
                artifactDescriptorRequest.setArtifact(artifact);
            }
            return new ModelRequest(mode, followRelocation, artifactDescriptorRequest);
        }
    }
}
