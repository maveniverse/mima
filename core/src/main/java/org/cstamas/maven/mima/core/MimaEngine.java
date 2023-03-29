package org.cstamas.maven.mima.core;

public interface MimaEngine extends MimaContextFactory {
    String name();

    int priority();
}
