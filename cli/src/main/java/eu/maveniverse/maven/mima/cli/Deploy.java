package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.nio.file.Path;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.SubArtifact;
import picocli.CommandLine;

/**
 * Deploy.
 */
@CommandLine.Command(name = "deploy", description = "Deploys Maven Artifacts")
public final class Deploy extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to install")
    private String gav;

    @CommandLine.Parameters(index = "1", description = "The artifact JAR file")
    private Path jar;

    @CommandLine.Parameters(index = "2", description = "The artifact POM file")
    private Path pom;

    @CommandLine.Parameters(index = "3", description = "The RemoteRepository baseUrl")
    private String repoUrl;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Deploying {}", gav);
        RepositorySystem system = context.repositorySystem();
        RepositorySystemSession session = context.repositorySystemSession();

        Artifact jarArtifact = new DefaultArtifact(gav);
        jarArtifact = jarArtifact.setFile(jar.toFile());

        Artifact pomArtifact = new SubArtifact(jarArtifact, "", "pom");
        pomArtifact = pomArtifact.setFile(pom.toFile());

        RemoteRepository remoteRepository = new RemoteRepository.Builder("target", "default", repoUrl).build();

        DeployRequest deployRequest = new DeployRequest();
        deployRequest.addArtifact(jarArtifact).addArtifact(pomArtifact).setRepository(remoteRepository);

        try {
            system.deploy(session, deployRequest);
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }
}
