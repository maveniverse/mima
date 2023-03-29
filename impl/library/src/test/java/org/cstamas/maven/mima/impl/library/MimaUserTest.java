package org.cstamas.maven.mima.impl.library;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import org.cstamas.maven.mima.core.MimaResolver;
import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.engines.standalone.StandaloneEngine;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.junit.jupiter.api.Test;

public class MimaUserTest {
    @Test
    public void simple() throws Exception {
        StandaloneEngine engine = new StandaloneEngine();

        MimaContextOverrides overrides = MimaContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple"))
                .build();
        MimaContext context = engine.create(overrides);
        MimaResolver resolver = new MimaResolver(context);

        DefaultArtifact artifact = new DefaultArtifact("junit:junit:4.13.2");
        String classpath = resolver.classpath(artifact);
        assertThat(classpath, notNullValue());
    }

    @Test
    public void simpleOffline() {
        StandaloneEngine engine = new StandaloneEngine();

        MimaContextOverrides overrides = MimaContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple-offline"))
                .offline(true)
                .build();
        MimaContext context = engine.create(overrides);
        MimaResolver resolver = new MimaResolver(context);

        DefaultArtifact artifact = new DefaultArtifact("junit:junit:4.13.2");
        assertThrows(DependencyResolutionException.class, () -> resolver.classpath(artifact));
    }
}
