package org.cstamas.maven.mima.context;

public interface MimaEngine extends MimaContextFactory {
    String name();

    int priority();

    boolean managedRepositorySystem();
}
