package eu.maveniverse.maven.mima.impl.library;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Classpath {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String classpath(ContextOverrides overrides, String artifactStr) throws DependencyResolutionException {
        requireNonNull(artifactStr);
        Runtime runtime = Runtimes.INSTANCE.getRuntime();

        // ad-hoc: create context w/ or w/o overrides
        // other way is to make this class manage context or manage context outside it
        // depends what you need: one shot or reuse of MIMA instance
        try (Context context = runtime.create(overrides)) {
            DefaultArtifact artifact = new DefaultArtifact(artifactStr);
            return doClasspath(context, artifact);
        }
    }

    private String doClasspath(Context context, Artifact artifact) throws DependencyResolutionException {
        logger.info("doClasspath: {}", context.remoteRepositories());
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

    public static void main(String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("g:a:v");
        }
        Classpath classpath = new Classpath();
        try {
            ContextOverrides overrides =
                    ContextOverrides.Builder.create().withUserSettings(true).build();

            String cp = classpath.classpath(overrides, args[0]);
            System.out.println("Classpath of " + args[0] + " is:");
            System.out.println(cp);
        } catch (DependencyResolutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
