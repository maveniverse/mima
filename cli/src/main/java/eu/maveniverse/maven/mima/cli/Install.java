package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.util.artifact.SubArtifact;
import picocli.CommandLine;

/**
 * Install.
 */
@CommandLine.Command(name = "install", description = "Installs Maven Artifacts")
public final class Install extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to install")
    private String gav;

    @CommandLine.Parameters(index = "1", description = "The artifact JAR file")
    private Path jar;

    @CommandLine.Parameters(index = "2", description = "The artifact POM file")
    private Path pom;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Installing {}", gav);

        Artifact jarArtifact = new DefaultArtifact(gav);
        jarArtifact = jarArtifact.setFile(jar.toFile());

        Artifact pomArtifact = new SubArtifact(jarArtifact, "", "pom");
        pomArtifact = pomArtifact.setFile(pom.toFile());

        InstallRequest installRequest = new InstallRequest();
        installRequest.addArtifact(jarArtifact).addArtifact(pomArtifact);

        try {
            context.repositorySystem().install(context.repositorySystemSession(), installRequest);
        } catch (InstallationException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }
}
