package org.cstamas.maven.mima.core;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public abstract class MimaEngineSupport implements MimaEngine {
    private final String name;

    private final int priority;

    private final boolean managedRepositorySystem;

    protected MimaEngineSupport(String name, int priority, boolean managedRepositorySystem) {
        this.name = requireNonNull(name);
        this.priority = priority;
        this.managedRepositorySystem = managedRepositorySystem;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean managedRepositorySystem() {
        return managedRepositorySystem;
    }

    protected MimaContext applyOverrides(
            MimaContextOverrides overrides,
            RepositorySystemSession repositorySystemSession,
            RepositorySystem repositorySystem,
            List<RemoteRepository> remoteRepositories) {
        if (overrides.isOffline()) {
            repositorySystemSession = new DefaultRepositorySystemSession(repositorySystemSession).setOffline(true);
        }
        return new MimaContext(
                managedRepositorySystem(), repositorySystem, repositorySystemSession, remoteRepositories);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='"
                + name + '\'' + ", priority="
                + priority + ", managedRepositorySystem="
                + managedRepositorySystem + '}';
    }
}
