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
    private final List<RemoteRepository> repositories;
    private final String requestContext;
    private final RequestTrace trace;

    private ModelRequest(
            Artifact artifact, List<RemoteRepository> repositories, String requestContext, RequestTrace trace) {
        this.artifact = requireNonNull(artifact);
        this.repositories = repositories;
        this.requestContext = requestContext == null ? "" : requestContext;
        this.trace = trace;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public String getRequestContext() {
        return requestContext;
    }

    public RequestTrace getTrace() {
        return trace;
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
            this.repositories = request.repositories;
            this.requestContext = request.requestContext;
            this.trace = request.trace;
        }

        public ModelRequest build() {
            return new ModelRequest(artifact, repositories, requestContext, trace);
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

        public Builder setRepositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
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
    }
}
