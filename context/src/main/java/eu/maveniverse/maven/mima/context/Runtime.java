package eu.maveniverse.maven.mima.context;

public interface Runtime {
    String name();

    int priority();

    String mavenVersion();

    boolean managedRepositorySystem();

    Context create(ContextOverrides overrides);
}
