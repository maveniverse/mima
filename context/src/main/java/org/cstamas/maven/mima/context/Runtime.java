package org.cstamas.maven.mima.context;

public interface Runtime extends ContextFactory {
    String name();

    int priority();

    boolean managedRepositorySystem();
}
