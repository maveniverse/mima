package org.cstamas.maven.mima.core;

import static java.util.Objects.requireNonNull;

import org.cstamas.maven.mima.context.MimaContext;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class MimaResolver {
    private final MimaContext context;

    public MimaResolver(MimaContext context) {
        this.context = requireNonNull(context);
    }

    public String classpath(String artifactoid) throws DependencyResolutionException {
        Dependency dependency = new Dependency(new DefaultArtifact(artifactoid), "runtime");
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
