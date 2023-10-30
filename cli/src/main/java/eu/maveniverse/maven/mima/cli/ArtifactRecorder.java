package eu.maveniverse.maven.mima.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

final class ArtifactRecorder extends AbstractRepositoryListener {
    private final RemoteRepository sentinel = new RemoteRepository.Builder("none", "default", "fake").build();
    private final ConcurrentHashMap<RemoteRepository, ArrayList<Artifact>> artifactsMap = new ConcurrentHashMap<>();

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (event.getException() == null) {
            RemoteRepository repository = event.getRepository() instanceof RemoteRepository
                    ? (RemoteRepository) event.getRepository()
                    : sentinel;
            artifactsMap.computeIfAbsent(repository, k -> new ArrayList<>()).add(event.getArtifact());
        }
    }

    public RemoteRepository getSentinel() {
        return sentinel;
    }

    public Map<RemoteRepository, ArrayList<Artifact>> getArtifactsMap() {
        return artifactsMap;
    }

    public List<Artifact> getAllArtifacts() {
        return artifactsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public Set<Artifact> getUniqueArtifacts() {
        return new HashSet<>(getAllArtifacts());
    }
}
