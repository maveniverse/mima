package eu.maveniverse.maven.mima.cli;

import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import picocli.CommandLine;

/**
 * Resolver.
 */
@CommandLine.Command(name = "resolve", description = "Resolves Maven Artifacts")
public final class Resolve extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to resolve")
    private String gav;

    @Override
    public Integer call() {
        doWithContext(context -> {
            logger.info("Resolving {}", gav);
            RepositorySystem system = context.repositorySystem();
            RepositorySystemSession session = context.repositorySystemSession();

            Artifact artifact = new DefaultArtifact(gav);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
            collectRequest.setRepositories(context.remoteRepositories());
            DependencyRequest dependencyRequest =
                    new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));

            try {
                List<ArtifactResult> artifactResults =
                        system.resolveDependencies(session, dependencyRequest).getArtifactResults();

                for (ArtifactResult artifactResult : artifactResults) {
                    logger.info(
                            "{} -> {}",
                            artifactResult.getArtifact(),
                            artifactResult.getArtifact().getFile());
                }
            } catch (DependencyResolutionException e) {
                throw new RuntimeException(e);
            }
        });
        return 1;
    }
}
