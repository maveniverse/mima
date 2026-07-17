package eu.maveniverse.maven.mima.runtime.shared;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Lookup;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StandaloneRuntimeSupportTest {
    private RepositorySystem repositorySystem;
    private RepositorySystemSession.SessionBuilder sessionBuilder;
    private RepositorySystemSession.CloseableSession session;

    @BeforeEach
    void prepare() {
        repositorySystem = Mockito.mock(RepositorySystem.class);
        sessionBuilder = Mockito.mock(RepositorySystemSession.SessionBuilder.class);
        session = Mockito.mock(RepositorySystemSession.CloseableSession.class);

        when(repositorySystem.createSessionBuilder()).thenReturn(sessionBuilder);
        when(sessionBuilder.withRepositorySystemSession(any())).thenReturn(sessionBuilder);
        when(sessionBuilder.build()).thenReturn(session);
    }

    @Test
    void itPropagatesNullMavenSystemHome() {
        RuntimeSupport runtimeSupport = new StandaloneRuntimeSupport("test", 999) {
            @Override
            public boolean managedRepositorySystem() {
                return false;
            }

            @Override
            public Context create(ContextOverrides overrides) {
                return null;
            }

            @Override
            protected void customizeLocalRepositoryManager(
                    Context context,
                    ContextOverrides contextOverrides,
                    MavenUserHome mavenUserHome,
                    RepositorySystemSession.SessionBuilder session) {
                // Intentionally skipped
            }

            @Override
            protected List<RemoteRepository> customizeRemoteRepositories(
                    ContextOverrides contextOverrides, List<RemoteRepository> remoteRepositories) {
                // Intentionally skipped
                return remoteRepositories;
            }
        };

        Context context = new Context(
                runtimeSupport,
                ContextOverrides.create().build(),
                Paths.get("/test"),
                runtimeSupport.defaultMavenUserHome(),
                null,
                repositorySystem,
                session,
                null,
                new Lookup() {
                    @Override
                    public <T> Optional<T> lookup(Class<T> type) {
                        return Optional.empty();
                    }

                    @Override
                    public <T> Optional<T> lookup(Class<T> type, String name) {
                        return Optional.empty();
                    }
                },
                null);

        ContextOverrides overrides =
                ContextOverrides.create().offline(Boolean.TRUE).build();

        Assertions.assertDoesNotThrow(() -> runtimeSupport.customizeContext(overrides, context, false));
    }
}
