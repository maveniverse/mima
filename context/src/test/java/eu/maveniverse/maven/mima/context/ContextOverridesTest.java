package eu.maveniverse.maven.mima.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class ContextOverridesTest {
    @Test
    void contextBuilderContext() {
        ContextOverrides co1 = ContextOverrides.Builder.create().build();
        ContextOverrides co2 = co1.toBuilder().build();
        assertEquals(co1, co2);
    }

    @Test
    void contextBuilderLocalRepositoryOverrideNullContext() {
        ContextOverrides co1 = ContextOverrides.Builder.create().build();
        ContextOverrides co2 = co1.toBuilder().withLocalRepositoryOverride(null).build();
        assertEquals(co1, co2);
    }

    @Test
    void contextBuilderLocalRepositoryOverrideNonNullContext() {
        ContextOverrides co1 = ContextOverrides.Builder.create().build();
        ContextOverrides co2 =
                co1.toBuilder().withLocalRepositoryOverride(Paths.get("foo")).build();
        assertNotEquals(co1, co2);

        ContextOverrides co3 = co2.toBuilder().withLocalRepositoryOverride(null).build();
        assertEquals(co1, co3);
    }
}
