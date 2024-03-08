/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.SubArtifact;
import picocli.CommandLine;

/**
 * Deploys an artifact into remote repository.
 */
@CommandLine.Command(name = "deploy", description = "Deploys Maven Artifacts")
public final class Deploy extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to deploy")
    private String gav;

    @CommandLine.Parameters(index = "1", description = "The artifact JAR file")
    private Path jar;

    @CommandLine.Parameters(index = "2", description = "The artifact POM file")
    private Path pom;

    @CommandLine.Parameters(index = "3", description = "The RemoteRepository spec (id::url)")
    private String remoteRepositorySpec;

    @Override
    protected Integer doCall(Context context) throws DeploymentException {
        Artifact jarArtifact = new DefaultArtifact(gav);
        jarArtifact = jarArtifact.setFile(jar.toFile());

        Artifact pomArtifact = new SubArtifact(jarArtifact, "", "pom");
        pomArtifact = pomArtifact.setFile(pom.toFile());

        RemoteRepository remoteRepository = getContext()
                .repositorySystem()
                .newDeploymentRepository(
                        getRepositorySystemSession(), buildRemoteRepositoryFromSpec(remoteRepositorySpec));

        DeployRequest deployRequest = new DeployRequest();
        deployRequest.addArtifact(jarArtifact).addArtifact(pomArtifact).setRepository(remoteRepository);

        verbose("Deploying {}", deployRequest);
        context.repositorySystem().deploy(getRepositorySystemSession(), deployRequest);

        info("Deployed {}", gav);
        return 0;
    }
}
