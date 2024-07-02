/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import picocli.CommandLine;

/**
 * Records resolved artifacts.
 */
@CommandLine.Command(name = "record", description = "Records resolved Maven Artifacts")
public final class Record extends ResolverCommandSupport {

    @Override
    protected Integer doCall(Context context) throws DependencyResolutionException {
        ArtifactRecorder recorder = new ArtifactRecorder();
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(getRepositorySystemSession());
        session.setRepositoryListener(
                session.getRepositoryListener() != null
                        ? ChainedRepositoryListener.newInstance(session.getRepositoryListener(), recorder)
                        : recorder);
        push(ArtifactRecorder.class.getName(), recorder);
        push(RepositorySystemSession.class.getName(), session);

        info("Recording...");
        return 0;
    }
}
