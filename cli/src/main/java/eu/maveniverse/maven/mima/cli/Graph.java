package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.manager.DefaultDependencyManager;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.manager.NoopDependencyManager;
import org.eclipse.aether.util.graph.manager.TransitiveDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import picocli.CommandLine;

/**
 * Graph.
 */
@CommandLine.Command(name = "graph", description = "Displays dependency graph")
public final class Graph extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to graph")
    private String gav;

    @CommandLine.Option(
            names = {"--dependencyManager"},
            defaultValue = "classic",
            description = "Dependency manager to use (classic, default, noop, transitive)")
    private String dependencyManager;

    @CommandLine.Option(
            names = {"--excludeScopes"},
            defaultValue = JavaScopes.TEST,
            split = ",",
            description = "Scopes to exclude (default is 'test')")
    private String[] excludeScopes;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Collecting {}", gav);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
        session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

        session.setDependencyManager(dependencyManager());

        session.setDependencySelector(new AndDependencySelector(
                new ScopeDependencySelector(excludeScopes),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()));
        session.setDependencyTraverser(new FatArtifactTraverser());

        session.setDependencyGraphTransformer(new ChainedDependencyGraphTransformer(
                new ConflictResolver(
                        new NearestVersionSelector(), new JavaScopeSelector(),
                        new SimpleOptionalitySelector(), new JavaScopeDeriver()),
                new JavaDependencyContextRefiner()));

        Artifact artifact = new DefaultArtifact(gav);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.setRepositories(context.remoteRepositories());

        try {
            logger.info("");
            context.repositorySystem()
                    .collectDependencies(session, collectRequest)
                    .getRoot()
                    .accept(new DependencyGraphDumper(logger::info));
        } catch (DependencyCollectionException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }

    private DependencyManager dependencyManager() {
        if ("classic".equalsIgnoreCase(dependencyManager)) {
            return new ClassicDependencyManager();
        } else if ("default".equalsIgnoreCase(dependencyManager)) {
            return new DefaultDependencyManager();
        } else if ("noop".equalsIgnoreCase(dependencyManager)) {
            return new NoopDependencyManager();
        } else if ("transitive".equalsIgnoreCase(dependencyManager)) {
            return new TransitiveDependencyManager();
        } else {
            throw new IllegalArgumentException("Unknown dependency manager: " + dependencyManager);
        }
    }
}
