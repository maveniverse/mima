package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
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
import picocli.CommandLine;

/**
 * Resolve.
 */
@CommandLine.Command(name = "resolve", description = "Resolves Maven Artifacts")
public final class Resolve extends CommandSupport {

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
    protected Integer doCall(Context context) {
        logger.info("Resolving {}", gav);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        ArtifactCollector collector = new ArtifactCollector();
        session.setRepositoryListener(collector);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(new DefaultArtifact(gav), JavaScopes.COMPILE));
        collectRequest.setRepositories(context.remoteRepositories());
        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(scope));

        try {
            context.repositorySystem().resolveDependencies(session, dependencyRequest);

            ArrayList<ArtifactRequest> artifactRequests = new ArrayList<>();
            for (Map.Entry<RemoteRepository, ArrayList<Artifact>> entry : collector.artifacts.entrySet()) {
                List<RemoteRepository> repositories =
                        entry.getKey() == collector.sentinel ? null : Collections.singletonList(entry.getKey());
                for (Artifact artifact : entry.getValue()) {
                    if ("jar".equals(artifact.getExtension()) && "".equals(artifact.getClassifier())) {
                        if (sources) {
                            artifactRequests.add(new ArtifactRequest(
                                    new SubArtifact(artifact, "sources", "jar"), repositories, null));
                        }
                        if (javadoc) {
                            artifactRequests.add(new ArtifactRequest(
                                    new SubArtifact(artifact, "javadoc", "jar"), repositories, null));
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
            for (Artifact artifact : collector.artifacts.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList())) {
                logger.info("{} -> {}", artifact, artifact.getFile());
            }
        } catch (DependencyResolutionException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }

    private static final class ArtifactCollector extends AbstractRepositoryListener {
        private final RemoteRepository sentinel = new RemoteRepository.Builder("none", "default", "fake").build();
        private final ConcurrentHashMap<RemoteRepository, ArrayList<Artifact>> artifacts = new ConcurrentHashMap<>();

        @Override
        public void artifactResolved(RepositoryEvent event) {
            if (event.getException() == null) {
                RemoteRepository repository = event.getRepository() instanceof RemoteRepository
                        ? (RemoteRepository) event.getRepository()
                        : sentinel;
                artifacts.computeIfAbsent(repository, k -> new ArrayList<>()).add(event.getArtifact());
            }
        }
    }
}
