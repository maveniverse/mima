package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

public final class RuntimeVersions {

    public static final String UNKNOWN = "(unknown)";

    private final String resolverVersion;

    private final String mavenVersion;

    public RuntimeVersions(String resolverVersion, String mavenVersion) {
        this.resolverVersion = requireNonNull(resolverVersion);
        this.mavenVersion = requireNonNull(mavenVersion);
    }

    public String resolverVersion() {
        return resolverVersion;
    }

    public String mavenVersion() {
        return mavenVersion;
    }
}
