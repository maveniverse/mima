package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import picocli.CommandLine;

/**
 * Resolve.
 */
@CommandLine.Command(name = "resolve", description = "Resolves Maven Artifacts")
public final class Resolve extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to resolve")
    private String gav;

    @CommandLine.Option(
            names = {"-cp", "--classpath"},
            description = "Show classpath ready to copy-paste")
    private boolean classpath;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Resolving {}", gav);

        Artifact artifact = new DefaultArtifact(gav);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
        collectRequest.setRepositories(context.remoteRepositories());
        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));

        try {
            DependencyResult dependencyResult = context.repositorySystem()
                    .resolveDependencies(context.repositorySystemSession(), dependencyRequest);

            for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
                logger.info(
                        "{} -> {}",
                        artifactResult.getArtifact(),
                        artifactResult.getArtifact().getFile());
            }

            if (classpath) {
                PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
                dependencyResult.getRoot().accept(nlg);
                logger.info("");
                logger.info("classpath: {}", nlg.getClassPath());
            }
        } catch (DependencyResolutionException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }
}
