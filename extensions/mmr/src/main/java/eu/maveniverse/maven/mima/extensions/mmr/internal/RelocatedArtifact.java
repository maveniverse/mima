/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr.internal;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.Artifact;

/**
 * @author Benjamin Bentmann
 */
public final class RelocatedArtifact extends AbstractArtifact {

    private final Artifact artifact;

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String message;

    public RelocatedArtifact(Artifact artifact, String groupId, String artifactId, String version, String message) {
        this.artifact = Objects.requireNonNull(artifact, "artifact cannot be null");
        this.groupId = (groupId != null && !groupId.isEmpty()) ? groupId : null;
        this.artifactId = (artifactId != null && !artifactId.isEmpty()) ? artifactId : null;
        this.version = (version != null && !version.isEmpty()) ? version : null;
        this.message = (message != null && !message.isEmpty()) ? message : null;
    }

    @Override
    public String getGroupId() {
        if (groupId != null) {
            return groupId;
        } else {
            return artifact.getGroupId();
        }
    }

    @Override
    public String getArtifactId() {
        if (artifactId != null) {
            return artifactId;
        } else {
            return artifact.getArtifactId();
        }
    }

    @Override
    public String getVersion() {
        if (version != null) {
            return version;
        } else {
            return artifact.getVersion();
        }
    }

    // Revise these three methods when MRESOLVER-233 is delivered
    @Override
    public Artifact setVersion(String version) {
        String current = getVersion();
        if (current.equals(version) || (version == null && current.length() <= 0)) {
            return this;
        }
        return new RelocatedArtifact(artifact, groupId, artifactId, version, message);
    }

    @Override
    public Artifact setFile(File file) {
        File current = getFile();
        if (Objects.equals(current, file)) {
            return this;
        }
        return new RelocatedArtifact(artifact.setFile(file), groupId, artifactId, version, message);
    }

    @Override
    public Artifact setProperties(Map<String, String> properties) {
        Map<String, String> current = getProperties();
        if (current.equals(properties) || (properties == null && current.isEmpty())) {
            return this;
        }
        return new RelocatedArtifact(artifact.setProperties(properties), groupId, artifactId, version, message);
    }

    @Override
    public String getClassifier() {
        return artifact.getClassifier();
    }

    @Override
    public String getExtension() {
        return artifact.getExtension();
    }

    @Override
    public File getFile() {
        return artifact.getFile();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return artifact.getProperty(key, defaultValue);
    }

    @Override
    public Map<String, String> getProperties() {
        return artifact.getProperties();
    }

    public String getMessage() {
        return message;
    }
}
