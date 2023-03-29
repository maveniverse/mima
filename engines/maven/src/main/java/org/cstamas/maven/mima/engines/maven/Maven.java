package org.cstamas.maven.mima.engines.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.cstamas.maven.mima.context.MimaContext;
import org.cstamas.maven.mima.context.MimaContextOverrides;
import org.cstamas.maven.mima.engine.EngineSupport;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class Maven extends EngineSupport {

    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    @Inject
    public Maven(RepositorySystem repositorySystem, MavenSession mavenSession) {
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
    }

    @Override
    public MimaContext create(MimaContextOverrides overrides) {
        return new MimaContext(
                mavenSession.getRepositorySession(),
                repositorySystem,
                mavenSession.getCurrentProject().getRemotePluginRepositories());
    }
}
