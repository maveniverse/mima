/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.maven.internal;

import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.runtime.maven.MavenRuntime;
import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Nullable;

@Named
@EagerSingleton
@Component(role = Bootstrap.class, instantiationStrategy = "load-on-start")
public class Bootstrap {
    @Inject
    public Bootstrap(@Nullable MavenRuntime mavenEngine) {
        if (mavenEngine != null) {
            Runtimes.INSTANCE.registerRuntime(mavenEngine);
        }
    }
}
