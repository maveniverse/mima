package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

public final class RuntimeVersions {

    public static final String UNKNOWN = "(unknown)";

    private final String mavenVersion;

    public RuntimeVersions(String mavenVersion) {
        this.mavenVersion = requireNonNull(mavenVersion);
    }

    public String mavenVersion() {
        return mavenVersion;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "mavenVersion='" + mavenVersion + '\'' + '}';
    }
}
