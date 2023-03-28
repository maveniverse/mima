package org.cstamas.maven.mima.engines.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.cstamas.maven.mima.context.MimaContext;
import org.cstamas.maven.mima.engine.Engine;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class Maven implements Engine {

    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    @Inject
    public Maven(RepositorySystem repositorySystem, MavenSession mavenSession) {
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
    }

    @Override
    public MimaContext create() {
        return new MavenMimaContext(
                mavenSession.getRepositorySession(),
                repositorySystem,
                mavenSession.getCurrentProject().getRemotePluginRepositories());
    }
}
