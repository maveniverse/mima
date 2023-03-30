package eu.maveniverse.maven.mima.runtime.maven;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.RuntimeSupport;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class MavenRuntime extends RuntimeSupport {
    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    @Inject
    public MavenRuntime(RepositorySystem repositorySystem, MavenSession mavenSession) {
        super("embedded-maven", 10, false);
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        return customizeContext(
                overrides,
                new Context(
                        false,
                        repositorySystem,
                        mavenSession.getRepositorySession(),
                        mavenSession.getCurrentProject().getRemoteProjectRepositories()),
                false);
    }
}
