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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven Model Reader, an extension that is able to read POM Models at various levels.
 * <p>
 * This component resembles {@link org.eclipse.aether.impl.ArtifactDescriptorReader} somewhat, but have
 * notable differences:
 * <ul>
 *     <li>Does not follow redirections: it will read artifact you asked for. To follow redirections you may want to
 *     perform it manually: inspect the effective model and based on {@link org.apache.maven.model.Relocation}
 *     issue another request for relocation target.</li>
 *     <li>Does not obey {@link RepositorySystemSession#getArtifactDescriptorPolicy()}, if asked artifact does not
 *     exists, it will fail.</li>
 *     <li>If passed in {@link Artifact} is resolved ({@link Artifact#getFile()} returns non-{@code null} value), then
 *     model will be read from that file directly. Naturally, to be able to build the model, the possible
 *     parent and other POMs must be resolvable.</li>
 * </ul>
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
     * Reads POM as {@link ArtifactDescriptorResult}. This is just convenience method, if someone is interested in
     */
    public ArtifactDescriptorResult readArtifactDescriptorResult(ArtifactDescriptorRequest request, ModelLevel mode)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        requireNonNull(request, "request");
        requireNonNull(mode, "mode");
        return readModel(new ModelRequest.Builder()
                        .setArtifactDescriptorRequest(request)
                        .build())
                .toArtifactDescriptorResult(mode);
    }

    /**
     * Reads POM as {@link Model}.
     */
    public Model readModel(ArtifactDescriptorRequest request, ModelLevel mode)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        requireNonNull(request, "request");
        requireNonNull(mode, "mode");
        return readModel(new ModelRequest.Builder()
                        .setArtifactDescriptorRequest(request)
                        .build())
                .toModel(mode);
    }

    /**
     * Reads POM as {@link ModelResponse}.
     */
    public ModelResponse readModel(ModelRequest request)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        requireNonNull(request, "request");
        return artifactDescriptorReader.readArtifactDescriptor(session, request);
    }
}
