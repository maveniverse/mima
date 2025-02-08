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
        this.requestContext = requestContext == null ? "" : requestContext;
        this.trace = trace;
        this.repositories = repositories;
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

        /**
         * Make possible to point at a POM anywhere on file system, to have it built. Naturally, all the required
         * bits like parent POM, imported POM must be still resolvable (from local or remote repositories).
         */
        public Builder setPomFile(Path pomFile) {
            requireNonNull(pomFile);
            return setArtifact(new DefaultArtifact("irrelevant:irrelevant:irrelevant").setFile(pomFile.toFile()));
        }

        /**
         * Sets the artifact whose POM we want to build model for. The artifact must be resolvable from local or
         * remote repositories.
         */
        public Builder setArtifact(Artifact artifact) {
            requireNonNull(artifact);
            this.artifact = artifact;
            return this;
        }

        /**
         * Optionally, user may want to override context "root" repositories with own set (ie appended or totally new
         * list of repositories).
         */
        public Builder setRepositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        /**
         * Sets the request context for bookkeeping purposes.
         */
        public Builder setRequestContext(String requestContext) {
            this.requestContext = requestContext;
            return this;
        }

        /**
         * Sets the request trace for bookkeeping purposes.
         */
        public Builder setTrace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }
    }
}
