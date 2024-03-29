/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

/**
 * Support.
 */
public abstract class ResolverCommandSupport extends CommandSupport {

    protected RepositorySystemSession getRepositorySystemSession() {
        return (RepositorySystemSession) getOrCreate(
                RepositorySystemSession.class.getName(), () -> getContext().repositorySystemSession());
    }

    protected RemoteRepository buildRemoteRepositoryFromSpec(String remoteRepositorySpec) {
        String[] parts = remoteRepositorySpec.split("::");
        if (parts.length == 1) {
            return new RemoteRepository.Builder("mima", "default", parts[0]).build();
        } else if (parts.length == 2) {
            return new RemoteRepository.Builder(parts[0], "default", parts[1]).build();
        } else {
            throw new IllegalArgumentException("Invalid remote repository spec");
        }
    }

    protected java.util.List<Dependency> importBoms(Context context, String... boms)
            throws ArtifactDescriptorException {
        HashSet<String> keys = new HashSet<>();
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(false, false));
        ArrayList<Dependency> managedDependencies = new ArrayList<>();
        for (String bomGav : boms) {
            if ("".equals(bomGav)) {
                continue;
            }
            Artifact bom = new DefaultArtifact(bomGav);
            ArtifactDescriptorRequest artifactDescriptorRequest =
                    new ArtifactDescriptorRequest(bom, context.remoteRepositories(), "");
            ArtifactDescriptorResult artifactDescriptorResult =
                    context.repositorySystem().readArtifactDescriptor(session, artifactDescriptorRequest);
            artifactDescriptorResult.getManagedDependencies().forEach(d -> {
                if (keys.add(ArtifactIdUtils.toVersionlessId(d.getArtifact()))) {
                    managedDependencies.add(d);
                } else {
                    info("W: BOM {} introduced an already managed dependency {}", bom, d);
                }
            });
        }
        return managedDependencies;
    }

    protected Artifact parseGav(String gav, java.util.List<Dependency> managedDependencies) {
        try {
            return new DefaultArtifact(gav);
        } catch (IllegalArgumentException e) {
            // assume it is g:a and we have v in depMgt section
            return managedDependencies.stream()
                    .map(Dependency::getArtifact)
                    .filter(a -> gav.equals(a.getGroupId() + ":" + a.getArtifactId()))
                    .findFirst()
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public final Integer call() {
        try (Context context = getContext()) {
            return doCall(context);
        } catch (Exception e) {
            error("Error", e);
            return 1;
        }
    }

    protected Integer doCall(Context context) throws Exception {
        throw new RuntimeException("Not implemented; you should override this method in subcommand");
    }
}
