package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;
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

public final class ContextOverrides {

    public static final RemoteRepository CENTRAL = new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2/")
            .setReleasePolicy(new RepositoryPolicy(
                    true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .setSnapshotPolicy(new RepositoryPolicy(
                    false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .build();

    public static final Path MAVEN_USER_HOME = Paths.get(System.getProperty("user.home"), ".m2");

    public static final Path USER_SETTINGS_XML = MAVEN_USER_HOME.resolve("settings.xml");

    public static final Path USER_SETTINGS_SECURITY_XML = MAVEN_USER_HOME.resolve("settings-security.xml");

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

        public static Builder create() {
            return new Builder();
        }

        public Builder systemProperties(Map<String, String> systemProperties) {
            if (systemProperties != null) {
                this.systemProperties = new HashMap<>(systemProperties);
            } else {
                this.systemProperties = null;
            }
            return this;
        }

        public Builder userProperties(Map<String, String> userProperties) {
            if (userProperties != null) {
                this.userProperties = new HashMap<>(userProperties);
            } else {
                this.userProperties = null;
            }
            return this;
        }

        public Builder configProperties(Map<String, Object> configProperties) {
            if (configProperties != null) {
                this.configProperties = new HashMap<>(configProperties);
            } else {
                this.configProperties = null;
            }
            return this;
        }

        public Builder repositories(List<RemoteRepository> repositories) {
            if (repositories != null) {
                this.repositories = new ArrayList<>(repositories);
            } else {
                this.repositories = null;
            }
            return this;
        }

        public Builder addRepository(RemoteRepository repository) {
            requireNonNull(repository);
            if (this.repositories == null) {
                this.repositories = new ArrayList<>();
            }
            this.repositories.add(repository);
            return this;
        }

        public Builder appendRepositories(boolean appendRepositories) {
            this.appendRepositories = appendRepositories;
            return this;
        }

        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder localRepository(Path localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        public Builder snapshotUpdatePolicy(SnapshotUpdatePolicy snapshotUpdatePolicy) {
            this.snapshotUpdatePolicy = snapshotUpdatePolicy;
            return this;
        }

        public Builder checksumPolicy(ChecksumPolicy checksumPolicy) {
            this.checksumPolicy = checksumPolicy;
            return this;
        }

        public Builder withUserSettings(boolean withUserSettings) {
            this.withUserSettings = withUserSettings;
            return this;
        }

        public Builder settingsXml(Path settingsXml) {
            this.settingsXml = settingsXml;
            return this;
        }

        public Builder repositoryListener(RepositoryListener repositoryListener) {
            this.repositoryListener = repositoryListener;
            return this;
        }

        public Builder transferListener(TransferListener transferListener) {
            this.transferListener = transferListener;
            return this;
        }

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

        /**
         * Collects (Maven) system properties as Maven does: it is a mixture of {@link System#getenv()} prefixed with
         * {@code "env."} and Java System properties.
         */
        public Map<String, String> defaultSystemProperties() {
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
}
