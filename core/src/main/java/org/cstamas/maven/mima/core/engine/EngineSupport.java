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

    protected EngineSupport(String name) {
        this.name = requireNonNull(name);
    }

    @Override
    public String name() {
        return name;
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
