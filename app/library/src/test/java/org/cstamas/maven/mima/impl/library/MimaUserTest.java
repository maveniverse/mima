package org.cstamas.maven.mima.impl.library;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import org.cstamas.maven.mima.context.MimaContextOverrides;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.junit.jupiter.api.Test;

public class MimaUserTest {
    @Test
    public void simple() throws Exception {
        Classpath classpath = new Classpath();

        MimaContextOverrides overrides = MimaContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple"))
                .build();

        String cp = classpath.classpath(overrides, "junit:junit:4.13.2");
        assertThat(cp, notNullValue());
    }

    @Test
    public void simpleOffline() {
        Classpath classpath = new Classpath();

        MimaContextOverrides overrides = MimaContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple-offline"))
                .offline(true)
                .build();

        assertThrows(DependencyResolutionException.class, () -> classpath.classpath(overrides, "junit:junit:4.13.2"));
    }
}
