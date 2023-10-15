package eu.maveniverse.maven.mima.runtime.standalonestatic;

import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.apache.maven.model.root.DefaultRootLocator;

/**
 * Override to customize.
 */
public class ProfileSelectorSupplier implements Supplier<ProfileSelector> {
    @Override
    public ProfileSelector get() {
        return new DefaultProfileSelector(Arrays.asList(
                new JdkVersionProfileActivator(),
                new PropertyProfileActivator(),
                new OperatingSystemProfileActivator(),
                new FileProfileActivator(new ProfileActivationFilePathInterpolator(
                        new DefaultPathTranslator(), new DefaultRootLocator()))));
    }
}
