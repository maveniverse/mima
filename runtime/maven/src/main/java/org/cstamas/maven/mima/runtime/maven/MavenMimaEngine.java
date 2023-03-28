package org.cstamas.maven.mima.runtime.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.cstamas.maven.mima.context.MimaContext;
import org.cstamas.maven.mima.context.MimaContextOverrides;
import org.cstamas.maven.mima.context.MimaEngineSupport;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class MavenMimaEngine extends MimaEngineSupport {

    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    @Inject
    public MavenMimaEngine(RepositorySystem repositorySystem, MavenSession mavenSession) {
        super("maven", 10, false);
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
