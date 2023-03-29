package org.cstamas.maven.mima.engines.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.cstamas.maven.mima.core.MimaContext;
import org.cstamas.maven.mima.core.MimaContextOverrides;
import org.cstamas.maven.mima.core.MimaEngineSupport;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class MavenMimaEngine extends MimaEngineSupport {

    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    @Inject
    public MavenMimaEngine(RepositorySystem repositorySystem, MavenSession mavenSession) {
        super("maven", 10);
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
    }

    @Override
    public MimaContext create(MimaContextOverrides overrides) {
        return applyOverrides(
                overrides,
                mavenSession.getRepositorySession(),
                repositorySystem,
                mavenSession.getCurrentProject().getRemoteProjectRepositories());
    }
}
