package eu.maveniverse.maven.mima.runtime.standalonesisu;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.RuntimeSupport;
import eu.maveniverse.maven.mima.runtime.standalonesisu.internal.SisuBooter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

public class StandaloneSisuRuntime extends RuntimeSupport {
    public StandaloneSisuRuntime() {
        super("standalone-sisu", 30, true);
    }

    @Override
    public Context create(ContextOverrides overrides) {

        HashMap<String, String> configurationProperties = new HashMap<>(System.getProperties().entrySet().stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue())));
        if (overrides.getUserProperties() != null) {
            configurationProperties.putAll(overrides.getUserProperties());
        }

        SisuBooter booter = SisuBooter.newRepositorySystem(configurationProperties);
        ArrayList<RemoteRepository> repositories = new ArrayList<>();
        repositories.add(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());
        return applyOverrides(
                overrides,
                newRepositorySystemSession(overrides, booter.repositorySystem),
                booter.repositorySystem,
                repositories);
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
