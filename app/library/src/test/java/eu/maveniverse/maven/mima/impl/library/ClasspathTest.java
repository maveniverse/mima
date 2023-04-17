package eu.maveniverse.maven.mima.impl.library;

import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class ClasspathTest {
    @Test
    public void simple() throws Exception {
        Classpath classpath = new Classpath();

        ContextOverrides overrides = ContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple"))
                .build();

        assertThrows(IllegalStateException.class, () -> classpath.classpath(overrides, "junit:junit:4.13.2"));
    }

    @Test
    public void simpleOffline() {
        Classpath classpath = new Classpath();

        ContextOverrides overrides = ContextOverrides.Builder.create()
                .localRepository(Paths.get("target/simple-offline"))
                .offline(true)
                .build();

        assertThrows(IllegalStateException.class, () -> classpath.classpath(overrides, "junit:junit:4.13.2"));
    }
}
