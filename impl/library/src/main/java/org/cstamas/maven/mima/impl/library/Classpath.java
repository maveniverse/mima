package org.cstamas.maven.mima.impl.library;

import java.nio.file.Paths;
import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.core.engine.Engine;
import org.cstamas.maven.mima.core.engine.Engines;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.DependencyResolutionException;

public class Classpath {

    public String classpath(MimaContextOverrides overrides, String artifactStr) throws DependencyResolutionException {
        Engine engine = Engines.INSTANCE.getEngine();

        MimaContext context = engine.create(overrides);
        MimaResolver resolver = new MimaResolver(context);

        DefaultArtifact artifact = new DefaultArtifact(artifactStr);
        return resolver.classpath(artifact);
    }

    public static void main(String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("g:a:v");
        }
        Classpath classpath = new Classpath();
        try {
            MimaContextOverrides overrides = MimaContextOverrides.Builder.create()
                    .localRepository(Paths.get("target/simple"))
                    .build();

            String cp = classpath.classpath(overrides, args[0]);
            System.out.println("Classpath of " + args[0] + " is:");
            System.out.println(cp);
        } catch (DependencyResolutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
