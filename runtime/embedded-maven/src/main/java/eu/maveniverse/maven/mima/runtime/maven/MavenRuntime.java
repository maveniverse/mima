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
        super("embedded-maven", 10, rt.getMavenVersion());
        this.repositorySystem = repositorySystem;
        this.mavenSessionProvider = mavenSessionProvider;
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
                        repositorySystem,
                        mavenSession.getRepositorySession(),
                        mavenSession.getCurrentProject().getRemoteProjectRepositories(),
                        null),
                false); // unmanaged context: close should NOT shut down repositorySystem
    }
}
