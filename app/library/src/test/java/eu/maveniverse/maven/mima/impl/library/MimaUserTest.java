package eu.maveniverse.maven.mima.impl.library;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import java.nio.file.Paths;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.junit.jupiter.api.Test;

public class MimaUserTest {
    @Test
    public void simple() throws Exception {
        Classpath classpath = new Classpath();

        ContextOverrides overrides = ContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple"))
                .build();

        String cp = classpath.classpath(overrides, "junit:junit:4.13.2");
        assertThat(cp, notNullValue());
    }

    @Test
    public void simpleOffline() {
        Classpath classpath = new Classpath();

        ContextOverrides overrides = ContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple-offline"))
                .offline(true)
                .build();

        assertThrows(DependencyResolutionException.class, () -> classpath.classpath(overrides, "junit:junit:4.13.2"));
    }
}
