/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

/**
 * A model resolver to assist building of dependency POMs. This resolver gives priority to those repositories that have
 * been initially specified and repositories discovered in dependency POMs are recessive merged into the search chain.
 *
 * @author Benjamin Bentmann
 * @see DefaultArtifactDescriptorReader
 */
public class ModelResolverImpl implements ModelResolver {
    private final RepositorySystem repositorySystem;

    private final RepositorySystemSession session;

    private final RequestTrace trace;

    private final String context;

    private List<RemoteRepository> repositories;

    private final List<RemoteRepository> externalRepositories;

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final Set<String> repositoryIds;

    public ModelResolverImpl(
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            RequestTrace trace,
            String context,
            RemoteRepositoryManager remoteRepositoryManager,
            List<RemoteRepository> repositories) {
        this.repositorySystem = repositorySystem;
        this.session = session;
        this.trace = trace;
        this.context = context;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.repositories = repositories;
        this.externalRepositories = Collections.unmodifiableList(new ArrayList<>(repositories));

        this.repositoryIds = new HashSet<>();
    }

    private ModelResolverImpl(ModelResolverImpl original) {
        this.repositorySystem = original.repositorySystem;
        this.session = original.session;
        this.trace = original.trace;
        this.context = original.context;
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.repositories = new ArrayList<>(original.repositories);
        this.externalRepositories = original.externalRepositories;
        this.repositoryIds = new HashSet<>(original.repositoryIds);
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {
        addRepository(repository, false);
    }

    @Override
    public void addRepository(final Repository repository, boolean replace) throws InvalidRepositoryException {
        if (session.isIgnoreArtifactDescriptorRepositories()) {
            return;
        }

        if (!repositoryIds.add(repository.getId())) {
            if (!replace) {
                return;
            }

            removeMatchingRepository(repositories, repository.getId());
        }

        List<RemoteRepository> newRepositories =
                Collections.singletonList(ArtifactDescriptorUtils.toRemoteRepository(repository));

        this.repositories = remoteRepositoryManager.aggregateRepositories(session, repositories, newRepositories, true);
    }

    private static void removeMatchingRepository(Iterable<RemoteRepository> repositories, final String id) {
        Iterator<RemoteRepository> iterator = repositories.iterator();
        while (iterator.hasNext()) {
            RemoteRepository remoteRepository = iterator.next();
            if (remoteRepository.getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    @Override
    public ModelResolver newCopy() {
        return new ModelResolverImpl(this);
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, context);
            request.setTrace(trace);
            pomArtifact = repositorySystem.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }

        File pomFile = pomArtifact.getFile();

        return new FileModelSource(pomFile);
    }

    @Override
    public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
        try {
            final Artifact artifact =
                    new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom", parent.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult =
                    repositorySystem.resolveVersionRange(session, versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format(
                                "No versions matched the requested parent version range '%s'", parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new UnresolvableModelException(
                        String.format(
                                "The requested parent version range '%s' does not specify an upper bound",
                                parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }

            parent.setVersion(versionRangeResult.getHighestVersion().toString());

            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        } catch (final VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), e);
        }
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
        try {
            final Artifact artifact = new DefaultArtifact(
                    dependency.getGroupId(), dependency.getArtifactId(), "", "pom", dependency.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult =
                    repositorySystem.resolveVersionRange(session, versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format(
                                "No versions matched the requested dependency version range '%s'",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-4463 core IT.
                throw new UnresolvableModelException(
                        String.format(
                                "The requested dependency version range '%s' does not specify an upper bound",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }

            dependency.setVersion(versionRangeResult.getHighestVersion().toString());

            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), e);
        }
    }
}
