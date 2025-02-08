/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Model request.
 */
public class ModelRequest {
    public static Builder builder() {
        return new Builder();
    }

    private final Artifact artifact;
    private final String requestContext;
    private final RequestTrace trace;
    private final List<RemoteRepository> repositories;

    private ModelRequest(
            Artifact artifact, String requestContext, RequestTrace trace, List<RemoteRepository> repositories) {
        this.artifact = requireNonNull(artifact);
        this.requestContext = requestContext == null ? "" : requestContext;
        this.trace = trace;
        this.repositories = repositories;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public String getRequestContext() {
        return requestContext;
    }

    public RequestTrace getTrace() {
        return trace;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private Artifact artifact;
        private List<RemoteRepository> repositories;
        private String requestContext;
        private RequestTrace trace;

        private Builder() {}

        private Builder(ModelRequest request) {
            this.artifact = request.artifact;
            this.requestContext = request.requestContext;
            this.trace = request.trace;
            this.repositories = request.repositories;
        }

        public ModelRequest build() {
            return new ModelRequest(artifact, requestContext, trace, repositories);
        }

        public Builder setPomFile(Path pomFile) {
            requireNonNull(pomFile);
            return setArtifact(new DefaultArtifact("irrelevant:irrelevant:irrelevant").setFile(pomFile.toFile()));
        }

        public Builder setArtifact(Artifact artifact) {
            requireNonNull(artifact);
            this.artifact = artifact;
            return this;
        }

        public Builder setRequestContext(String requestContext) {
            this.requestContext = requestContext;
            return this;
        }

        public Builder setTrace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        public Builder setRepositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }
    }
}
