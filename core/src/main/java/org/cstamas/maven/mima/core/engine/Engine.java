package org.cstamas.maven.mima.core.engine;

import org.cstamas.maven.mima.core.context.MimaContextFactory;

public interface Engine extends MimaContextFactory {
    String name();

    int priority();
}
