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
import eu.maveniverse.maven.mima.extensions.mmr.internal.MavenModelReaderImpl;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
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
 * This component resembles {@link org.eclipse.aether.RepositorySystem#readArtifactDescriptor(RepositorySystemSession, ArtifactDescriptorRequest)}
 * somewhat, but have notable differences:
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
 * The purpose of this extension is to provide insight into Maven models without the need to fiddle
 * with any of those things like Model builder and so on. Usages like some "analysis" or "introspection"
 * or "validation" come to mind for start.
 * <p>
 * Note: this extension and all classes in it are EXPERIMENTAL, use on your own risk!
 */
public class MavenModelReader {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MavenModelReaderImpl mavenModelReaderImpl;

    public MavenModelReader(Context context) {
        requireNonNull(context, "context");
        this.mavenModelReaderImpl = new MavenModelReaderImpl(context);
    }

    /**
     * Reads POM as {@link ArtifactDescriptorResult}. This is just convenience method.
     */
    public ArtifactDescriptorResult readArtifactDescriptorResult(ArtifactDescriptorRequest request, ModelLevel mode)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        requireNonNull(request, "request");
        requireNonNull(mode, "mode");
        return readModel(request).toArtifactDescriptorResult(mode);
    }

    /**
     * Reads POM as {@link Model}. This is just convenience method.
     */
    public Model readModel(ArtifactDescriptorRequest request, ModelLevel mode)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        requireNonNull(request, "request");
        requireNonNull(mode, "mode");
        return readModel(request).toModel(mode);
    }

    /**
     * Reads POM as {@link ModelResponse}.
     */
    public ModelResponse readModel(ArtifactDescriptorRequest request)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        requireNonNull(request, "request");
        return mavenModelReaderImpl.readModel(request);
    }
}
