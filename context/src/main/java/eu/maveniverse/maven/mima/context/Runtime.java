package eu.maveniverse.maven.mima.context;

/**
 * Runtime is a factory for {@link Context} instances.
 */
public interface Runtime {

    /**
     * The runtime name (mostly for keying purposes), never {@code null}.
     */
    String name();

    /**
     * The priority of runtime instance. Priorities use natural integer ordering.
     */
    int priority();

    /**
     * Returns a string representing Maven version this runtime uses, never {@code null}. This mostly stands for
     * "maven models" version, except when MIMA runs inside of Maven, when it carries the "actual Maven version".
     */
    String mavenVersion();

    /**
     * Returns {@code true} if this runtime creates managed repository system, that is opposite when MIMA runs
     * in Maven (or any other environment providing resolver), where it does not manage it, as hosting Maven or app
     * does. In general, you should always treat "root context" as explained in {@link Context} and your code will be
     * portable.
     */
    boolean managedRepositorySystem();

    /**
     * Creates a {@link Context} instance using passed in {@link ContextOverrides}, never returns {@code null}.
     */
    Context create(ContextOverrides overrides);
}
