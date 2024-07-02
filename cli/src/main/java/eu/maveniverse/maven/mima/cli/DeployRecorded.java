/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.util.Set;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import picocli.CommandLine;

/**
 * Deploys recorded artifacts to remote repository.
 */
@CommandLine.Command(name = "deployRecorded", description = "Deploys recorded Maven Artifacts")
public final class DeployRecorded extends ResolverCommandSupport {
    @CommandLine.Parameters(index = "0", description = "The RemoteRepository spec (id::url)")
    private String remoteRepositorySpec;

    @Override
    protected Integer doCall(Context context) throws DeploymentException {
        info("Deploying recorded");

        ArtifactRecorder recorder = (ArtifactRecorder) pop(ArtifactRecorder.class.getName());
        DeployRequest deployRequest = new DeployRequest();
        RemoteRepository remoteRepository = getContext()
                .repositorySystem()
                .newDeploymentRepository(
                        getRepositorySystemSession(), buildRemoteRepositoryFromSpec(remoteRepositorySpec));
        deployRequest.setRepository(remoteRepository);
        Set<Artifact> uniqueArtifacts = recorder.getUniqueArtifacts();
        uniqueArtifacts.forEach(deployRequest::addArtifact);

        context.repositorySystem().deploy(getRepositorySystemSession(), deployRequest);

        info("");
        info("Deployed recorded {} artifacts to {}", uniqueArtifacts.size(), remoteRepository);
        return 0;
    }
}
