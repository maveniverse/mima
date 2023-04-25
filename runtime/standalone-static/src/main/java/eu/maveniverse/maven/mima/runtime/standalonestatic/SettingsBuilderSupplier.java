package eu.maveniverse.maven.mima.runtime.standalonestatic;

import java.util.function.Supplier;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;

/**
 * Override to customize.
 */
public class SettingsBuilderSupplier implements Supplier<SettingsBuilder> {
    @Override
    public SettingsBuilder get() {
        return new DefaultSettingsBuilder(
                new DefaultSettingsReader(), new DefaultSettingsWriter(), new DefaultSettingsValidator());
    }
}
