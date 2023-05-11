package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
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

    public static final class MavenUserHome {
        private final Path mavenUserHome;

        private final Path settingsXmlOverride;

        private final Path settingsSecurityXmlOverride;

        private final Path localRepositoryOverride;

        public MavenUserHome() {
            this(DEFAULT_MAVEN_USER_HOME);
        }

        public MavenUserHome(Path mavenUserHome) {
            this(mavenUserHome, null, null, null);
        }

        public MavenUserHome(
                Path mavenUserHome,
                Path settingsXmlOverride,
                Path settingsSecurityXmlOverride,
                Path localRepositoryOverride) {
            this.mavenUserHome = requireNonNull(mavenUserHome);
            this.settingsXmlOverride = settingsXmlOverride;
            this.settingsSecurityXmlOverride = settingsSecurityXmlOverride;
            this.localRepositoryOverride = localRepositoryOverride;
        }

        public Path settingsXml() {
            if (settingsXmlOverride != null) {
                return settingsXmlOverride;
            }
            return mavenUserHome.resolve("settings.xml");
        }

        public Path settingsSecurityXml() {
            if (settingsSecurityXmlOverride != null) {
                return settingsSecurityXmlOverride;
            }
            return mavenUserHome.resolve("settings-security.xml");
        }

        public Path localRepository() {
            if (localRepositoryOverride != null) {
                return localRepositoryOverride;
            }
            return mavenUserHome.resolve("repository");
        }
    }

    public static final class MavenSystemHome {
        private final Path mavenSystemHome;

        public MavenSystemHome(Path mavenSystemHome) {
            this.mavenSystemHome = requireNonNull(mavenSystemHome);
        }

        public Path bin() {
            return mavenSystemHome.resolve("bin");
        }

        public Path boot() {
            return mavenSystemHome.resolve("boot");
        }

        public Path conf() {
            return mavenSystemHome.resolve("conf");
        }

        public Path lib() {
            return mavenSystemHome.resolve("lib");
        }

        public Path m2Conf() {
            return bin().resolve("m2.conf");
        }

        public Path mvn() {
            return bin().resolve("mvn");
        }

        public Path mvnCmd() {
            return bin().resolve("mvn.cmd");
        }

        public Path mvnDebug() {
            return bin().resolve("mvnDebug");
        }

        public Path mvnDebugCmd() {
            return bin().resolve("mvnDebug.cmd");
        }

        public Path settingsXml() {
            return conf().resolve("settings.xml");
        }

        public Path toolchainsXml() {
            return conf().resolve("toolchains.xml");
        }

        public Path confLogging() {
            return conf().resolve("logging");
        }

        public Path simpleloggerProperties() {
            return confLogging().resolve("simplelogger.properties");
        }

        public Path libExt() {
            return lib().resolve("ext");
        }
    }

    /**
     * Default path of Maven User Home.
     */
    public static final Path DEFAULT_MAVEN_USER_HOME = Paths.get(System.getProperty("user.home"), ".m2");

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

    private final SnapshotUpdatePolicy snapshotUpdatePolicy;

    private final ChecksumPolicy checksumPolicy;

    private final boolean withUserSettings;

    private final RepositoryListener repositoryListener;

    private final TransferListener transferListener;

    private final MavenUserHome mavenUserHome;

    private final MavenSystemHome mavenSystemHome;

    private ContextOverrides(
            final Map<String, String> systemProperties,
            final Map<String, String> userProperties,
            final Map<String, Object> configProperties,
            final List<RemoteRepository> repositories,
            final boolean appendRepositories,
            final boolean offline,
            final SnapshotUpdatePolicy snapshotUpdatePolicy,
            final ChecksumPolicy checksumPolicy,
            final boolean withUserSettings,
            final RepositoryListener repositoryListener,
            final TransferListener transferListener,
            final MavenUserHome mavenUserHome,
            final MavenSystemHome mavenSystemHome) {

        this.systemProperties = systemProperties;
        this.userProperties = userProperties;
        this.configProperties = configProperties;
        this.repositories = repositories;
        this.appendRepositories = appendRepositories;
        this.offline = offline;
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
        this.checksumPolicy = checksumPolicy;
        this.withUserSettings = withUserSettings;
        this.repositoryListener = repositoryListener;
        this.transferListener = transferListener;
        this.mavenUserHome = mavenUserHome;
        this.mavenSystemHome = mavenSystemHome;
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

    /**
     * @deprecated Use {@link #getMavenUserHome()} instead.
     */
    @Deprecated
    public Path getLocalRepository() {
        return getMavenUserHome().localRepository();
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

    /**
     * @deprecated Use {@link #getMavenUserHome()} instead.
     */
    @Deprecated
    public Path getSettingsXml() {
        return getMavenUserHome().settingsXml();
    }

    public RepositoryListener getRepositoryListener() {
        return repositoryListener;
    }

    public TransferListener getTransferListener() {
        return transferListener;
    }

    public MavenUserHome getMavenUserHome() {
        return mavenUserHome;
    }

    public MavenSystemHome getMavenSystemHome() {
        return mavenSystemHome;
    }

    public static final class Builder {
        private Map<String, String> systemProperties = defaultSystemProperties();

        private Map<String, String> userProperties = new HashMap<>();

        private Map<String, Object> configProperties = new HashMap<>();

        private List<RemoteRepository> repositories = Collections.singletonList(CENTRAL);

        private boolean appendRepositories = false;

        private boolean offline = false;

        private SnapshotUpdatePolicy snapshotUpdatePolicy = null;

        private ChecksumPolicy checksumPolicy = null;

        private boolean withUserSettings = false;

        private RepositoryListener repositoryListener = null;

        private TransferListener transferListener = null;

        private Path mavenUserHome = DEFAULT_MAVEN_USER_HOME;

        private Path settingsXmlOverride = null;

        private Path settingsSecurityXmlOverride = null;

        private Path localRepositoryOverride = null;

        private Path mavenSystemHome = null;

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
                this.systemProperties = new HashMap<>();
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
                this.userProperties = new HashMap<>();
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
                this.configProperties = new HashMap<>();
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
         *
         * @deprecated Use {@link #withLocalRepositoryOverride(Path)} instead.
         */
        @Deprecated
        public Builder localRepository(Path localRepository) {
            return withLocalRepositoryOverride(localRepository);
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
         *
         * @deprecated Use {@link #withSettingsXmlOverride(Path)} instead.
         */
        @Deprecated
        public Builder settingsXml(Path settingsXml) {
            return withSettingsXmlOverride(settingsXml);
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
         * Overrides Maven User Home, does not accept {@code null}.
         *
         * @since 2.1.0
         */
        public Builder withMavenUserHome(Path mavenUserHome) {
            requireNonNull(mavenUserHome);
            this.mavenUserHome = mavenUserHome;
            return this;
        }

        /**
         * Overrides Maven User settings.xml location.
         *
         * @since 2.1.0
         */
        public Builder withSettingsXmlOverride(Path settingsXmlOverride) {
            this.settingsXmlOverride = settingsXmlOverride;
            return this;
        }

        /**
         * Overrides Maven User settings-security.xml location.
         *
         * @since 2.1.0
         */
        public Builder withSettingsSecurityXmlOverride(Path settingsSecurityXmlOverride) {
            this.settingsSecurityXmlOverride = settingsSecurityXmlOverride;
            return this;
        }

        /**
         * Overrides Maven User local repository location.
         *
         * @since 2.1.0
         */
        public Builder withLocalRepositoryOverride(Path localRepositoryOverride) {
            this.localRepositoryOverride = localRepositoryOverride;
            return this;
        }

        /**
         * Sets Maven System Home location.
         *
         * @since 2.1.0
         */
        public Builder withMavenSystemHome(Path mavenSystemHome) {
            this.mavenSystemHome = mavenSystemHome;
            return this;
        }

        /**
         * Builds an immutable instance of {@link ContextOverrides} using so far applied settings and configuration.
         */
        public ContextOverrides build() {
            Map<String, Object> effectiveConfigProperties = new HashMap<>(systemProperties);
            effectiveConfigProperties.putAll(userProperties);
            effectiveConfigProperties.putAll(configProperties);

            Path effectiveLocalRepository = safeAbsolute(localRepositoryOverride);
            if (effectiveLocalRepository == null) {
                String localRepoPath = (String) effectiveConfigProperties.get("maven.repo.local");
                if (localRepoPath != null) {
                    effectiveLocalRepository = Paths.get(localRepoPath).toAbsolutePath();
                }
            }

            Path effectiveMavenSystemHome = safeAbsolute(mavenSystemHome);
            if (effectiveMavenSystemHome == null) {
                String mavenHome = (String) effectiveConfigProperties.get("maven.home");
                if (mavenHome == null) {
                    mavenHome = (String) effectiveConfigProperties.get("env.MAVEN_HOME");
                }
                if (mavenHome != null) {
                    effectiveMavenSystemHome = Paths.get(mavenHome).toAbsolutePath();
                }
            }

            return new ContextOverrides(
                    systemProperties,
                    userProperties,
                    effectiveConfigProperties,
                    repositories,
                    appendRepositories,
                    offline,
                    snapshotUpdatePolicy,
                    checksumPolicy,
                    withUserSettings,
                    repositoryListener,
                    transferListener,
                    new MavenUserHome(
                            mavenUserHome.toAbsolutePath(),
                            safeAbsolute(settingsXmlOverride),
                            safeAbsolute(settingsSecurityXmlOverride),
                            effectiveLocalRepository),
                    effectiveMavenSystemHome == null ? null : new MavenSystemHome(effectiveMavenSystemHome));
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

    /**
     * Helper to safely make nullable {@link Path} instances absolute.
     */
    private static Path safeAbsolute(Path path) {
        if (path == null) {
            return null;
        }
        return path.toAbsolutePath();
    }
}
