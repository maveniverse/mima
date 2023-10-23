package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DefaultDependencyManager;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
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

    @CommandLine.Parameters(index = "0", description = "The GAV to install")
    private String gav;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Collecting {}", gav);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
        session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

        // TODO: configuration for this
        session.setDependencyManager(new DefaultDependencyManager());

        // TODO: configuration for this
        session.setDependencySelector(new AndDependencySelector(
                new ScopeDependencySelector(JavaScopes.TEST),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()));
        session.setDependencyTraverser(new FatArtifactTraverser());

        // TODO: configuration for this
        DependencyGraphTransformer transformer = new ConflictResolver(
                new NearestVersionSelector(), new JavaScopeSelector(),
                new SimpleOptionalitySelector(), new JavaScopeDeriver());
        transformer = new ChainedDependencyGraphTransformer(transformer, new JavaDependencyContextRefiner());
        session.setDependencyGraphTransformer(transformer);

        Artifact artifact = new DefaultArtifact(gav);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.setRepositories(context.remoteRepositories());

        try {
            context.repositorySystem()
                    .collectDependencies(session, collectRequest)
                    .getRoot()
                    .accept(new DependencyGraphDumper(logger::info));
        } catch (DependencyCollectionException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }
}
