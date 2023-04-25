package eu.maveniverse.maven.mima.context;

public interface Runtime {
    String name();

    int priority();

    boolean managedRepositorySystem();

    RuntimeVersions runtimeVersions();

    Context create();

    Context create(ContextOverrides overrides);
}
