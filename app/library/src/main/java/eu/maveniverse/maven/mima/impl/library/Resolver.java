package eu.maveniverse.maven.mima.impl.library;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class Resolver {
    private final Context context;

    public Resolver(Context context) {
        this.context = requireNonNull(context);
    }

    public String classpath(Artifact artifact) throws DependencyResolutionException {
        requireNonNull(artifact);
        Dependency dependency = new Dependency(artifact, "runtime");
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(context.remoteRepositories());

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest(collectRequest);

        DependencyNode rootNode = context.repositorySystem()
                .resolveDependencies(context.repositorySystemSession(), dependencyRequest)
                .getRoot();

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        rootNode.accept(nlg);
        return nlg.getClassPath();
    }
}
