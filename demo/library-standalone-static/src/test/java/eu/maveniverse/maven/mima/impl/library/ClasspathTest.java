package eu.maveniverse.maven.mima.impl.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.nio.file.Paths;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.junit.jupiter.api.Test;

public class ClasspathTest {
    @Test
    public void simple() throws Exception {
        ContextOverrides overrides = ContextOverrides.Builder.create()
                .withLocalRepositoryOverride(Paths.get("target/simple"))
                .build();

        String cp = new Classpath().classpath(overrides, "junit:junit:4.13.2");
        assertNotNull(cp);
    }

    @Test
    public void simpleOffline() {
        ContextOverrides overrides = ContextOverrides.Builder.create()
                .withLocalRepositoryOverride(Paths.get("target/simpleOffline"))
                .offline(true)
                .build();

        assertThrows(
                DependencyResolutionException.class, () -> new Classpath().classpath(overrides, "junit:junit:4.13.2"));
    }

    @Test
    public void simpleEncrypted() {
        ContextOverrides overrides = ContextOverrides.Builder.create()
                .withMavenUserHome(Paths.get("target/test-classes/encrypted"))
                .withUserSettings(true)
                .withLocalRepositoryOverride(Paths.get("target/simpleEncrypted"))
                .build();

        try (Context context = Runtimes.INSTANCE.getRuntime().create(overrides)) {
            RemoteRepository repository = context.repositorySystem()
                    .newDeploymentRepository(
                            context.repositorySystemSession(),
                            new RemoteRepository.Builder("my-server", "default", "https://does.not.matter/").build());
            Authentication authentication = repository.getAuthentication();
            try (AuthenticationContext authContext =
                    AuthenticationContext.forRepository(context.repositorySystemSession(), repository)) {
                authentication.fill(authContext, AuthenticationContext.PASSWORD, null);
                String pw = authContext.get(AuthenticationContext.PASSWORD);
                assertEquals(pw, "server-secret");
            }
        }
    }

    @Test
    public void simpleEncryptedNoMaster() {
        ContextOverrides overrides = ContextOverrides.Builder.create()
                .withMavenUserHome(Paths.get("target/test-classes/encrypted-no-master"))
                .withUserSettings(true)
                .withLocalRepositoryOverride(Paths.get("target/simpleEncrypted"))
                .build();

        try (Context context = Runtimes.INSTANCE.getRuntime().create(overrides)) {
            RemoteRepository repository = context.repositorySystem()
                    .newDeploymentRepository(
                            context.repositorySystemSession(),
                            new RemoteRepository.Builder("my-server", "default", "https://does.not.matter/").build());
            Authentication authentication = repository.getAuthentication();
            try (AuthenticationContext authContext =
                    AuthenticationContext.forRepository(context.repositorySystemSession(), repository)) {
                authentication.fill(authContext, AuthenticationContext.PASSWORD, null);
                String pw = authContext.get(AuthenticationContext.PASSWORD);
                assertEquals(pw, "{BnJray6RajsHJ1EO0G6owUQBV3DNG/bWKiyipdTKeyA=}");
            }
        }
    }
}
