/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.cli;

import picocli.CommandLine;

/**
 * Dumps MIMA environment.
 */
@CommandLine.Command(name = "dump", description = "Dump MIMA environment")
public final class Dump extends CommandSupport {
    @Override
    public Integer call() {
        mayDumpEnv(getRuntime(), getContext(), true);
        return 0;
    }
}
