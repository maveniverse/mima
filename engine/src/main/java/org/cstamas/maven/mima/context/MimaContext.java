package org.cstamas.maven.mima.context;

import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public interface MimaContext {
    RepositorySystemSession rootSession();

    RepositorySystem repositorySystem();

    List<RemoteRepository> remoteRepositories();
}
