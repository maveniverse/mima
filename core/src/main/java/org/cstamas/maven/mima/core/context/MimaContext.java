package org.cstamas.maven.mima.core.context;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class MimaContext {
    private final RepositorySystemSession repositorySystemSession;

    private final RepositorySystem repositorySystem;

    private final List<RemoteRepository> remoteRepositories;

    public MimaContext(
            RepositorySystemSession repositorySystemSession,
            RepositorySystem repositorySystem,
            List<RemoteRepository> remoteRepositories) {
        this.repositorySystemSession = requireNonNull(repositorySystemSession);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.remoteRepositories = requireNonNull(remoteRepositories);
    }

    public RepositorySystemSession repositorySystemSession() {
        return repositorySystemSession;
    }

    public RepositorySystem repositorySystem() {
        return repositorySystem;
    }

    public List<RemoteRepository> remoteRepositories() {
        return remoteRepositories;
    }
}
