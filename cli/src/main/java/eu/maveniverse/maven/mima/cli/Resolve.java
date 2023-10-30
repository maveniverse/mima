package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import picocli.CommandLine;

/**
 * Resolve.
 */
@CommandLine.Command(name = "resolve", description = "Resolves Maven Artifacts")
public final class Resolve extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to resolve")
    private String gav;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Download sources JARs as well (best effort)")
    private boolean sources;

    @CommandLine.Option(
            names = {"--javadoc"},
            description = "Download javadoc JARs as well (best effort)")
    private boolean javadoc;

    @CommandLine.Option(
            names = {"--scope"},
            defaultValue = JavaScopes.COMPILE,
            description = "Scope to resolve")
    private String scope;

    @Override
    protected Integer doCall(Context context) throws DependencyResolutionException {
        logger.info("Resolving {}", gav);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(getRepositorySystemSession());
        ArtifactRecorder recorder = new ArtifactRecorder();
        session.setRepositoryListener(
                session.getRepositoryListener() != null
                        ? ChainedRepositoryListener.newInstance(session.getRepositoryListener(), recorder)
                        : recorder);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(new DefaultArtifact(gav), JavaScopes.COMPILE));
        collectRequest.setRepositories(context.remoteRepositories());
        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(scope));

        context.repositorySystem().resolveDependencies(session, dependencyRequest);

        ArrayList<ArtifactRequest> artifactRequests = new ArrayList<>();
        for (Map.Entry<RemoteRepository, ArrayList<Artifact>> entry :
                recorder.getArtifactsMap().entrySet()) {
            List<RemoteRepository> repositories =
                    entry.getKey() == recorder.getSentinel() ? null : Collections.singletonList(entry.getKey());
            for (Artifact artifact : entry.getValue()) {
                if ("jar".equals(artifact.getExtension()) && "".equals(artifact.getClassifier())) {
                    if (sources) {
                        artifactRequests.add(
                                new ArtifactRequest(new SubArtifact(artifact, "sources", "jar"), repositories, null));
                    }
                    if (javadoc) {
                        artifactRequests.add(
                                new ArtifactRequest(new SubArtifact(artifact, "javadoc", "jar"), repositories, null));
                    }
                }
            }
        }
        try {
            context.repositorySystem().resolveArtifacts(session, artifactRequests);
        } catch (ArtifactResolutionException e) {
            // log
        }

        logger.info("");
        if (verbose) {
            for (Artifact artifact : recorder.getAllArtifacts()) {
                logger.info("{} -> {}", artifact, artifact.getFile());
            }
        } else {
            logger.info("Resolved {} artifacts", recorder.getAllArtifacts().size());
        }
        return 0;
    }
}
