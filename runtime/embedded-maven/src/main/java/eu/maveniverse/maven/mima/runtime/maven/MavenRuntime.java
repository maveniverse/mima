package eu.maveniverse.maven.mima.runtime.maven;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public final class MavenRuntime extends RuntimeSupport {
    private final RepositorySystem repositorySystem;

    private final Provider<MavenSession> mavenSessionProvider;

    @Inject
    public MavenRuntime(
            RepositorySystem repositorySystem, Provider<MavenSession> mavenSessionProvider, RuntimeInformation rt) {
        super(
                "embedded-maven",
                discoverArtifactVersion("eu.maveniverse.maven.mima.runtime", "embedded-maven", UNKNOWN),
                10,
                mavenVersion(rt));
        this.repositorySystem = repositorySystem;
        this.mavenSessionProvider = mavenSessionProvider;
    }

    private static String mavenVersion(RuntimeInformation runtimeInformation) {
        String mavenVersion = runtimeInformation.getMavenVersion();
        if (mavenVersion == null || mavenVersion.trim().isEmpty()) {
            return UNKNOWN;
        }
        return mavenVersion;
    }

    @Override
    public boolean managedRepositorySystem() {
        return false;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        MavenSession mavenSession = mavenSessionProvider.get();
        return customizeContext(
                this,
                overrides,
                new Context(
                        this,
                        overrides,
                        repositorySystem,
                        mavenSession.getRepositorySession(),
                        mavenSession.getCurrentProject().getRemoteProjectRepositories(),
                        null),
                false); // unmanaged context: close should NOT shut down repositorySystem
    }
}
