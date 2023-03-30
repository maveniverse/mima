package eu.maveniverse.maven.mima.runtime.maven;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.RuntimeSupport;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.settings.building.SettingsBuilder;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class MavenRuntime extends RuntimeSupport {
    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    private final ModelBuilder modelBuilder;

    private final SettingsBuilder settingsBuilder;

    @Inject
    public MavenRuntime(
            RepositorySystem repositorySystem,
            MavenSession mavenSession,
            ModelBuilder modelBuilder,
            SettingsBuilder settingsBuilder) {
        super("embedded-maven", 10, false);
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
        this.modelBuilder = modelBuilder;
        this.settingsBuilder = settingsBuilder;
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
