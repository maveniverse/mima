package org.cstamas.maven.mima.runtime.sisu;

import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.building.SettingsBuilder;
import org.cstamas.maven.mima.context.Context;
import org.cstamas.maven.mima.context.ContextOverrides;
import org.cstamas.maven.mima.context.RuntimeSupport;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named
public class EmbeddedSisuRuntime extends RuntimeSupport {
    private final RepositorySystem repositorySystem;

    private final ModelBuilder modelBuilder;

    private final SettingsBuilder settingsBuilder;

    @Inject
    public EmbeddedSisuRuntime(
            RepositorySystem repositorySystem, ModelBuilder modelBuilder, SettingsBuilder settingsBuilder) {
        super("embedded-sisu", 15, false);
        this.repositorySystem = repositorySystem;
        this.modelBuilder = modelBuilder;
        this.settingsBuilder = settingsBuilder;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        ArrayList<RemoteRepository> repositories = new ArrayList<>();
        repositories.add(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());
        return applyOverrides(
                overrides, newRepositorySystemSession(overrides, repositorySystem), repositorySystem, repositories);
    }

    private static DefaultRepositorySystemSession newRepositorySystemSession(
            ContextOverrides overrides, RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo;
        if (overrides.getLocalRepository() != null) {
            localRepo = new LocalRepository(overrides.getLocalRepository().toFile());
        } else {
            localRepo = new LocalRepository("target/local-repo");
        }
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        if (overrides.getTransferListener() != null) {
            session.setTransferListener(overrides.getTransferListener());
        }
        if (overrides.getRepositoryListener() != null) {
            session.setRepositoryListener(overrides.getRepositoryListener());
        }

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }
}
