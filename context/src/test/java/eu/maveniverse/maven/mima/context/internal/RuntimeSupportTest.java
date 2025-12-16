package eu.maveniverse.maven.mima.context.internal;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Lookup;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RuntimeSupportTest {
    @Test
    void itPropagatesNullMavenSystemHome() {
        RuntimeSupport runtimeSupport = new RuntimeSupport("test", "123", 999, "123", "123") {
            @Override
            public boolean managedRepositorySystem() {
                return false;
            }

            @Override
            public Context create(ContextOverrides overrides) {
                return null;
            }

            @Override
            protected void customizeLocalRepositoryManager(Context context, DefaultRepositorySystemSession session) {
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
                Mockito.mock(RepositorySystem.class),
                Mockito.mock(RepositorySystemSession.class),
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
