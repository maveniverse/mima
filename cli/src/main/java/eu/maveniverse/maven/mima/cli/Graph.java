package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
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
public final class Graph extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to graph")
    private String gav;

    @CommandLine.Option(
            names = {"--excludeScopes"},
            defaultValue = JavaScopes.TEST,
            split = ",",
            description = "Scopes to exclude (default is 'test')")
    private String[] excludeScopes;

    @Override
    protected Integer doCall(Context context) throws DependencyCollectionException {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
        session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

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

        context.repositorySystem()
                .collectDependencies(session, collectRequest)
                .getRoot()
                .accept(new DependencyGraphDumper(this::info));
        return 0;
    }
}
