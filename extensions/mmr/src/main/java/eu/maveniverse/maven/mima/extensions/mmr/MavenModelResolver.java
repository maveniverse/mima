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

public class MavenModelResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArtifactDescriptorReaderImpl artifactDescriptorReader;
    private final RepositorySystemSession session;

    public MavenModelResolver(Context context) {
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

    public ArtifactDescriptorResult readEffectiveArtifactDescriptor(ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {
        return artifactDescriptorReader.readEffectiveArtifactDescriptor(session, request);
    }

    public ArtifactDescriptorResult readRawArtifactDescriptor(ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {
        return artifactDescriptorReader.readRawArtifactDescriptor(session, request);
    }

    public Model readEffectiveModel(ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        return (Model) readEffectiveArtifactDescriptor(request).getProperties().get("model");
    }

    public Model readRawModel(ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        return (Model) readRawArtifactDescriptor(request).getProperties().get("model");
    }
}
