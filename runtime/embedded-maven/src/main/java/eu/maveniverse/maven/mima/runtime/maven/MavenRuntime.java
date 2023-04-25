package eu.maveniverse.maven.mima.runtime.maven;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.RuntimeSupport;
import eu.maveniverse.maven.mima.context.RuntimeVersions;
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

    private final RuntimeVersions runtimeVersions;

    @Inject
    public MavenRuntime(
            RepositorySystem repositorySystem, Provider<MavenSession> mavenSessionProvider, RuntimeInformation rt) {
        super("embedded-maven", 10);
        this.repositorySystem = repositorySystem;
        this.mavenSessionProvider = mavenSessionProvider;
        this.runtimeVersions = new RuntimeVersions(rt.getMavenVersion());
    }

    @Override
    public boolean managedRepositorySystem() {
        return false;
    }

    @Override
    public RuntimeVersions runtimeVersions() {
        return runtimeVersions;
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
