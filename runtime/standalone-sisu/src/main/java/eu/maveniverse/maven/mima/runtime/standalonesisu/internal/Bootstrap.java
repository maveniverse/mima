/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.runtime.standalonesisu.StandaloneSisuRuntime;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Nullable;

@Named
@EagerSingleton
public class Bootstrap {
    @Inject
    public Bootstrap(@Nullable StandaloneSisuRuntime standaloneSisuRuntime) {
        if (standaloneSisuRuntime != null) {
            Runtimes.INSTANCE.registerRuntime(standaloneSisuRuntime);
        }
    }
}
