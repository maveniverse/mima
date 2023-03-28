package org.cstamas.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class MimaContext implements Closeable {
    private final boolean managedRepositorySystem;
    private final RepositorySystem repositorySystem;

    private final RepositorySystemSession repositorySystemSession;

    private final List<RemoteRepository> remoteRepositories;

    public MimaContext(
            boolean managedRepositorySystem,
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySystemSession,
            List<RemoteRepository> remoteRepositories) {
        this.managedRepositorySystem = managedRepositorySystem;
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

    @Override
    public void close() {
        // repositorySystemSession.close();
        if (managedRepositorySystem) {
            repositorySystem.shutdown();
        }
    }
}
