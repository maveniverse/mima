package eu.maveniverse.maven.mima.context;

import static java.util.stream.Collectors.toMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.transfer.TransferListener;

/**
 * Overrides applicable to {@link Context} creation. To create instances, use the {@link Builder}.
 * <p>
 * Values set in overrides are "ultimate overrides", they override everything, if set.
 */
public final class ContextOverrides {

    /**
     * Default Maven Central repository.
     */
    public static final RemoteRepository CENTRAL = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2/")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .setSnapshotPolicy(new RepositoryPolicy(
                    false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .build();

    /**
     * Default path of Maven User Home.
     */
    public static final Path MAVEN_USER_HOME = Paths.get(System.getProperty("user.home"), ".m2");

    /**
     * Default path of Maven User Settings.
     */
    public static final Path USER_SETTINGS_XML = MAVEN_USER_HOME.resolve("settings.xml");

    /**
     * Default path of Maven User Settings Security.
     */
    public static final Path USER_SETTINGS_SECURITY_XML = MAVEN_USER_HOME.resolve("settings-security.xml");

    /**
     * Default path of Maven User Local Repository.
     */
    public static final Path USER_LOCAL_REPOSITORY = MAVEN_USER_HOME.resolve("repository");

    public enum SnapshotUpdatePolicy {
        ALWAYS,
        NEVER
    }

    public enum ChecksumPolicy {
        FAIL,
        WARN,
        IGNORE
    }

    private final Map<String, String> systemProperties;

    private final Map<String, String> userProperties;

    private final Map<String, Object> configProperties;

    private final List<RemoteRepository> repositories;

    private final boolean appendRepositories;

    private final boolean offline;

    private final Path localRepository;

    private final SnapshotUpdatePolicy snapshotUpdatePolicy;

    private final ChecksumPolicy checksumPolicy;

    private final boolean withUserSettings;

    private final Path settingsXml;

    private final RepositoryListener repositoryListener;

    private final TransferListener transferListener;

    private ContextOverrides(Builder builder) {
        this.systemProperties = builder.systemProperties;
        this.userProperties = builder.userProperties;
        this.configProperties = builder.configProperties;
        this.repositories = builder.repositories;
        this.appendRepositories = builder.appendRepositories;
        this.offline = builder.offline;
        this.localRepository = builder.localRepository;
        this.snapshotUpdatePolicy = builder.snapshotUpdatePolicy;
        this.checksumPolicy = builder.checksumPolicy;
        this.withUserSettings = builder.withUserSettings;
        this.settingsXml = builder.settingsXml;
        this.repositoryListener = builder.repositoryListener;
        this.transferListener = builder.transferListener;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    public Map<String, Object> getConfigProperties() {
        return configProperties;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public boolean isAppendRepositories() {
        return appendRepositories;
    }

    public boolean isOffline() {
        return offline;
    }

    public Path getLocalRepository() {
        return localRepository;
    }

    public SnapshotUpdatePolicy getSnapshotUpdatePolicy() {
        return snapshotUpdatePolicy;
    }

    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    public boolean isWithUserSettings() {
        return withUserSettings;
    }

    public Path getSettingsXml() {
        return settingsXml;
    }

    public RepositoryListener getRepositoryListener() {
        return repositoryListener;
    }

    public TransferListener getTransferListener() {
        return transferListener;
    }

    public static final class Builder {
        private Map<String, String> systemProperties;

        private Map<String, String> userProperties;

        private Map<String, Object> configProperties;

        private List<RemoteRepository> repositories;

        private boolean appendRepositories;

        private boolean offline;

        private Path localRepository;

        private SnapshotUpdatePolicy snapshotUpdatePolicy;

        private ChecksumPolicy checksumPolicy;

        private boolean withUserSettings;

        private Path settingsXml;

        private RepositoryListener repositoryListener;

        private TransferListener transferListener;

        /**
         * Creates a "default" builder instance (that will NOT discover {@code settings.xml}).
         * <p>
         * Note: if you want to obey "Maven environment", you must invoke {@link #withUserSettings(boolean)} with
         * {@code true} parameter at least.
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * Sets Maven System Properties to be used. Users usually don't want to tamper with this.
         * <p>
         * For defaults see {@link #defaultSystemProperties()}.
         */
        public Builder systemProperties(Map<String, String> systemProperties) {
            if (systemProperties != null) {
                this.systemProperties = new HashMap<>(systemProperties);
            } else {
                this.systemProperties = null;
            }
            return this;
        }

        /**
         * Sets Maven User Properties to be used. These override the Maven System Properties.
         */
        public Builder userProperties(Map<String, String> userProperties) {
            if (userProperties != null) {
                this.userProperties = new HashMap<>(userProperties);
            } else {
                this.userProperties = null;
            }
            return this;
        }

        /**
         * Sets Maven Configuration Properties to be used. These accept {@link Object} values, and may be used for
         * advanced configuration of some Resolver aspect. Usually users don't want to tamper with these.
         * <p>
         * In case you want to tamper with these, you must ensure you create config properties the "right way":
         * <pre>config properties = system properties + user properties</pre>
         */
        public Builder configProperties(Map<String, Object> configProperties) {
            if (configProperties != null) {
                this.configProperties = new HashMap<>(configProperties);
            } else {
                this.configProperties = null;
            }
            return this;
        }

        /**
         * Sets the list of {@link RemoteRepository} instance you want to use. The list may replace or append the
         * list of repositories coming from Maven, see {@link #appendRepositories(boolean)}.
         * <p>
         * If {@link #withUserSettings(boolean)} invoked with {@code true}, the {@code settings.xml} discovered
         * repositories (and many more) will be used to create context. Also, in case when MIMA runs within Maven,
         * the current project repositories will be provided.
         */
        public Builder repositories(List<RemoteRepository> repositories) {
            if (repositories != null) {
                this.repositories = new ArrayList<>(repositories);
            } else {
                this.repositories = null;
            }
            return this;
        }

        /**
         * If {@code true}, the {@link #repositories(List)} provided non-null list will be appended to repositories
         * coming from Maven (read from user {@code settings.xml} or current project), otherwise they are replacing
         * them. Default is {@code false}.
         */
        public Builder appendRepositories(boolean appendRepositories) {
            this.appendRepositories = appendRepositories;
            return this;
        }

        /**
         * Sets session offline.
         */
        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        /**
         * Overrides the (default ot discovered) location of local repository. This path "wins" always, even if
         * {@link #withUserSettings(boolean)} was invoked with {@code true} and it contains alternate local repository
         * path.
         */
        public Builder localRepository(Path localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        /**
         * Sets the snapshot update policy.
         */
        public Builder snapshotUpdatePolicy(SnapshotUpdatePolicy snapshotUpdatePolicy) {
            this.snapshotUpdatePolicy = snapshotUpdatePolicy;
            return this;
        }

        /**
         * Sets the checksum update policy.
         */
        public Builder checksumPolicy(ChecksumPolicy checksumPolicy) {
            this.checksumPolicy = checksumPolicy;
            return this;
        }

        /**
         * Enables or disables use of {@code settings.xml}, used to find out location of local repository,
         * authentication, remote repositories and many more.
         */
        public Builder withUserSettings(boolean withUserSettings) {
            this.withUserSettings = withUserSettings;
            return this;
        }

        /**
         * Overrides the default location of {@code settings.xml}. Setting this method only, without invoking
         * {@link #withUserSettings(boolean)} with {@code true}, makes the passed in path to this method ignored.
         */
        public Builder settingsXml(Path settingsXml) {
            this.settingsXml = settingsXml;
            return this;
        }

        /**
         * Sets {@link RepositoryListener} instance to be used.
         */
        public Builder repositoryListener(RepositoryListener repositoryListener) {
            this.repositoryListener = repositoryListener;
            return this;
        }

        /**
         * Sets {@link TransferListener} instance to be used.
         */
        public Builder transferListener(TransferListener transferListener) {
            this.transferListener = transferListener;
            return this;
        }

        /**
         * Builds an immutable instance of {@link ContextOverrides} using so far applied settings and configuration.
         */
        public ContextOverrides build() {
            if (systemProperties == null) {
                systemProperties = defaultSystemProperties();
            }

            if (configProperties == null) {
                configProperties = new HashMap<>();
                configProperties.putAll(systemProperties);
                if (userProperties != null) {
                    configProperties.putAll(userProperties);
                }
            }

            if (localRepository == null) {
                String localRepoPath = (String) configProperties.get("maven.repo.local");
                if (localRepoPath != null) {
                    localRepository = Paths.get(localRepoPath);
                }
            }
            return new ContextOverrides(this);
        }
    }

    /**
     * Collects (Maven) system properties as Maven does: it is a mixture of {@link System#getenv()} prefixed with
     * {@code "env."} and Java System properties.
     */
    public static Map<String, String> defaultSystemProperties() {
        HashMap<String, String> result = new HashMap<>();
        // Env variables prefixed with "env."
        result.putAll(System.getenv().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>("env." + e.getKey(), e.getValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
        // Java System properties
        result.putAll(System.getProperties().entrySet().stream()
                .collect(toMap(e -> (String) e.getKey(), e -> (String) e.getValue())));

        return result;
    }
}
