package org.cstamas.maven.mima.runtime.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.cstamas.maven.mima.context.Context;
import org.cstamas.maven.mima.context.ContextOverrides;
import org.cstamas.maven.mima.context.RuntimeSupport;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class MavenRuntime extends RuntimeSupport {
    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    @Inject
    public MavenRuntime(RepositorySystem repositorySystem, MavenSession mavenSession) {
        super("maven", 10, false);
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        return applyOverrides(
                overrides,
                mavenSession.getRepositorySession(),
                repositorySystem,
                mavenSession.getCurrentProject().getRemoteProjectRepositories());
    }
}
