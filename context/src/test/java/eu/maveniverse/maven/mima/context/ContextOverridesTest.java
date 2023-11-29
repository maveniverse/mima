package eu.maveniverse.maven.mima.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Paths;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ContextOverridesTest {
    @Test
    void contextBuilderContext() {
        ContextOverrides co1 = ContextOverrides.create().build();
        ContextOverrides co2 = co1.toBuilder().build();
        assertEquals(co1, co2);
    }

    @Test
    void contextBuilderLocalRepositoryOverrideNullContext() {
        ContextOverrides co1 = ContextOverrides.create().build();
        ContextOverrides co2 = co1.toBuilder().withLocalRepositoryOverride(null).build();
        assertEquals(co1, co2);
    }

    @Test
    void contextBuilderLocalRepositoryOverrideNonNullContext() {
        ContextOverrides co1 = ContextOverrides.create().build();
        ContextOverrides co2 =
                co1.toBuilder().withLocalRepositoryOverride(Paths.get("foo")).build();
        assertNotEquals(co1, co2);

        ContextOverrides co3 = co2.toBuilder().withLocalRepositoryOverride(null).build();
        assertEquals(co1, co3);
    }

    @Test
    void userProperties() {
        HashMap<String, String> userProperties = new HashMap<>();
        userProperties.put("foo", "bar");

        ContextOverrides co1 =
                ContextOverrides.create().userProperties(userProperties).build();
        ContextOverrides co2 = co1.toBuilder().build();
        ContextOverrides co3 = co2.toBuilder().userProperties(null).build();
        userProperties.put("foo", "baz");
        ContextOverrides co4 = co3.toBuilder().userProperties(userProperties).build();
        userProperties.put("foo", "bar");
        ContextOverrides co5 = co4.toBuilder().userProperties(userProperties).build();

        assertEquals(co1, co2);
        assertEquals(co1.getUserProperties().get("foo"), "bar");
        assertEquals(co2.getUserProperties().get("foo"), "bar");
        assertNotEquals(co1, co3);
        assertEquals(co3.getUserProperties().get("foo"), null);
        assertNotEquals(co1, co4);
        assertEquals(co4.getUserProperties().get("foo"), "baz");
        assertEquals(co1, co5);
        assertEquals(co5.getUserProperties().get("foo"), "bar");
    }
}
