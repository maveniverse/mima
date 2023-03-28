package org.cstamas.maven.mima.engines.maven;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.cstamas.maven.mima.context.MimaContext;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class MavenMimaContext implements MimaContext {
    private final RepositorySystemSession repositorySystemSession;

    private final RepositorySystem repositorySystem;

    private final List<RemoteRepository> remoteRepositories;

    public MavenMimaContext(
            RepositorySystemSession repositorySystemSession,
            RepositorySystem repositorySystem,
            List<RemoteRepository> remoteRepositories) {
        this.repositorySystemSession = requireNonNull(repositorySystemSession);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.remoteRepositories = requireNonNull(remoteRepositories);
    }

    @Override
    public RepositorySystemSession repositorySystemSession() {
        return repositorySystemSession;
    }

    @Override
    public RepositorySystem repositorySystem() {
        return repositorySystem;
    }

    @Override
    public List<RemoteRepository> remoteRepositories() {
        return remoteRepositories;
    }
}
