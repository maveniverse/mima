package org.cstamas.maven.mima.core.engine;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public abstract class EngineSupport implements Engine {
    private final String name;

    private final int priority;

    protected EngineSupport(String name, int priority) {
        this.name = requireNonNull(name);
        this.priority = priority;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int priority() {
        return priority;
    }

    protected MimaContext applyOverrides(
            MimaContextOverrides overrides,
            RepositorySystemSession repositorySystemSession,
            RepositorySystem repositorySystem,
            List<RemoteRepository> remoteRepositories) {
        if (overrides.isOffline()) {
            repositorySystemSession = new DefaultRepositorySystemSession(repositorySystemSession).setOffline(true);
        }
        return new MimaContext(repositorySystemSession, repositorySystem, remoteRepositories);
    }
}
