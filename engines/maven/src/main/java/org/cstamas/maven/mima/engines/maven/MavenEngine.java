package org.cstamas.maven.mima.engines.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.core.engine.EngineSupport;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class MavenEngine extends EngineSupport {

    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    @Inject
    public MavenEngine(RepositorySystem repositorySystem, MavenSession mavenSession) {
        super("maven");
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
    }

    @Override
    public MimaContext create(MimaContextOverrides overrides) {
        return applyOverrides(
                overrides,
                mavenSession.getRepositorySession(),
                repositorySystem,
                mavenSession.getCurrentProject().getRemotePluginRepositories());
    }
}
