package eu.maveniverse.maven.mima.runtime.standalonestatic;

import java.util.function.Supplier;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;

/**
 * Override to customize.
 */
public class ProfileSelectorSupplier implements Supplier<ProfileSelector> {
    @Override
    public ProfileSelector get() {
        return new DefaultProfileSelector()
                .addProfileActivator(new JdkVersionProfileActivator())
                .addProfileActivator(new PropertyProfileActivator())
                .addProfileActivator(new OperatingSystemProfileActivator())
                .addProfileActivator(new FileProfileActivator()
                        .setProfileActivationFilePathInterpolator(new ProfileActivationFilePathInterpolator()
                                .setPathTranslator(new DefaultPathTranslator())));
    }
}
