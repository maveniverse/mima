/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import java.io.Closeable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.sisu.bean.LifecycleManager;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

@Singleton
@Named
public class SisuBooter implements Closeable {
    @Inject
    public RepositorySystem repositorySystem;

    @Inject
    public SettingsBuilder settingsBuilder;

    @Inject
    public SettingsDecrypter settingsDecrypter;

    @Inject
    public ProfileSelector profileSelector;

    @Inject
    public LifecycleManager lifecycleManager;

    @Inject
    public MutableBeanLocator locator;

    @Override
    public void close() {
        try {
            repositorySystem.shutdown();
        } finally {
            lifecycleManager.unmanage();
            locator.clear();
        }
    }

    public static SisuBooter newSisuBooter(PreBoot preBoot) {
        final Module app = Main.wire(BeanScanning.CACHE, new AbstractModule() {
            @Override
            protected void configure() {
                bind(PreBoot.class).toInstance(preBoot);
            }
        });
        return Guice.createInjector(app).getInstance(SisuBooter.class);
    }
}
