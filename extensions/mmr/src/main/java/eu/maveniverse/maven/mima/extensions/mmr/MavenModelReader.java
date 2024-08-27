/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.extensions.mmr.internal.ArtifactDescriptorReaderImpl;
import eu.maveniverse.maven.mima.extensions.mmr.internal.DefaultModelCache;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuilder;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven Model Reader, an extension that is able to read POM Models at various levels.
 */
public class MavenModelReader {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArtifactDescriptorReaderImpl artifactDescriptorReader;
    private final RepositorySystemSession session;

    public MavenModelReader(Context context) {
        requireNonNull(context, "context");
        this.artifactDescriptorReader = new ArtifactDescriptorReaderImpl(
                context.repositorySystem(),
                context.lookup()
                        .lookup(RemoteRepositoryManager.class)
                        .orElseThrow(() -> new IllegalStateException("RemoteRepositoryManager not available")),
                context.lookup()
                        .lookup(ModelBuilder.class)
                        .orElseThrow(() -> new IllegalStateException("ModelBuilder not available")),
                context.lookup()
                        .lookup(RepositoryEventDispatcher.class)
                        .orElseThrow(() -> new IllegalStateException("RepositoryEventDispatcher not available")),
                DefaultModelCache::newInstance);
        this.session = context.repositorySystemSession();
    }

    /**
     * Reads POM as {@link ArtifactDescriptorResult}.
     */
    public ArtifactDescriptorResult readArtifactDescriptor(ArtifactDescriptorRequest request, MavenModelReaderMode mode)
            throws ArtifactDescriptorException {
        requireNonNull(request, "request");
        requireNonNull(mode, "mode");
        return artifactDescriptorReader.readArtifactDescriptor(session, request, mode);
    }

    /**
     * Reads POM as {@link Model}.
     */
    public Model readModel(ArtifactDescriptorRequest request, MavenModelReaderMode mode)
            throws ArtifactDescriptorException {
        requireNonNull(request, "request");
        requireNonNull(mode, "mode");
        return (Model) readArtifactDescriptor(request, mode).getProperties().get("model");
    }
}
