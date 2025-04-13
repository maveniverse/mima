/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
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
