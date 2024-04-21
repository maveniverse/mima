/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.artifact.ArtifactType;
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
                    true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .setSnapshotPolicy(new RepositoryPolicy(
                    false, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN))
            .build();

    public enum SnapshotUpdatePolicy {
        ALWAYS,
        NEVER
    }

    public enum ChecksumPolicy {
        FAIL,
        WARN,
        IGNORE
    }

    public enum AddRepositoriesOp {
        PREPEND,
        APPEND,
        REPLACE
    }

    private final Path basedirOverride;

    private final Map<String, String> systemProperties;

    private final Map<String, String> userProperties;

    private final Map<String, Object> configProperties;

    private final List<RemoteRepository> repositories;

    private final AddRepositoriesOp addRepositoriesOp;

    private final List<ArtifactType> extraArtifactTypes;

    private final boolean offline;

    private final SnapshotUpdatePolicy snapshotUpdatePolicy;

    private final ChecksumPolicy checksumPolicy;

    private final boolean withUserSettings;

    private final List<String> activeProfileIds;

    private final List<String> inactiveProfileIds;

    private final RepositoryListener repositoryListener;

    private final TransferListener transferListener;

    private final Path mavenUserHomeOverride;

    private final Path userSettingsXmlOverride;

    private final Path userSettingsSecurityXmlOverride;

    private final Path userToolchainsXmlOverride;

    private final Path localRepositoryOverride;

    private final Path mavenSystemHomeOverride;

    private final Path globalSettingsXmlOverride;

    private final Path globalToolchainsXmlOverride;

    private final Object effectiveSettings;

    private final Object effectiveSettingsMixin;

    private ContextOverrides(
            final Path basedirOverride,
            final Map<String, String> systemProperties,
            final Map<String, String> userProperties,
            final Map<String, Object> configProperties,
            final List<RemoteRepository> repositories,
            final AddRepositoriesOp addRepositoriesOp,
            final List<ArtifactType> extraArtifactTypes,
            final boolean offline,
            final SnapshotUpdatePolicy snapshotUpdatePolicy,
            final ChecksumPolicy checksumPolicy,
            final boolean withUserSettings,
            final List<String> activeProfileIds,
            final List<String> inactiveProfileIds,
            final RepositoryListener repositoryListener,
            final TransferListener transferListener,
            final Path mavenUserHomeOverride,
            final Path userSettingsXmlOverride,
            final Path userSettingsSecurityXmlOverride,
            final Path userToolchainsXmlOverride,
            final Path localRepositoryOverride,
            final Path mavenSystemHomeOverride,
            final Path globalSettingsXmlOverride,
            final Path globalToolchainsXmlOverride,
            final Object effectiveSettings,
            final Object effectiveSettingsMixin) {

        this.basedirOverride = basedirOverride;
        this.systemProperties = Collections.unmodifiableMap(systemProperties);
        this.userProperties = Collections.unmodifiableMap(userProperties);
        this.configProperties = Collections.unmodifiableMap(configProperties);
        this.repositories = Collections.unmodifiableList(repositories);
        this.addRepositoriesOp = requireNonNull(addRepositoriesOp);
        this.extraArtifactTypes = requireNonNull(extraArtifactTypes);
        this.offline = offline;
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
        this.checksumPolicy = checksumPolicy;
        this.withUserSettings = withUserSettings;
        this.activeProfileIds = Collections.unmodifiableList(activeProfileIds);
        this.inactiveProfileIds = Collections.unmodifiableList(inactiveProfileIds);
        this.repositoryListener = repositoryListener;
        this.transferListener = transferListener;
        this.mavenUserHomeOverride = mavenUserHomeOverride;
        this.userSettingsXmlOverride = userSettingsXmlOverride;
        this.userSettingsSecurityXmlOverride = userSettingsSecurityXmlOverride;
        this.userToolchainsXmlOverride = userToolchainsXmlOverride;
        this.localRepositoryOverride = localRepositoryOverride;
        this.mavenSystemHomeOverride = mavenSystemHomeOverride;
        this.globalSettingsXmlOverride = globalSettingsXmlOverride;
        this.globalToolchainsXmlOverride = globalToolchainsXmlOverride;
        this.effectiveSettings = effectiveSettings;
        this.effectiveSettingsMixin = effectiveSettingsMixin;
    }

    /**
     * Returns the basedirOverride, or {@code null}.
     */
    public Path getBasedirOverride() {
        return basedirOverride;
    }

    /**
     * Maven System Properties map, never {@code null}.
     */
    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    /**
     * Maven User Properties map, never {@code null}.
     */
    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    /**
     * Maven Config properties, never {@code null}.
     */
    public Map<String, Object> getConfigProperties() {
        return configProperties;
    }

    /**
     * User added list of repositories, never {@code null}.
     */
    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    /**
     * How to handle {@link #getRepositories()} list, never {@code null}.
     *
     * @since 2.4.0
     */
    public AddRepositoriesOp addRepositoriesOp() {
        return addRepositoriesOp;
    }

    /**
     * User added list of artifact types, never {@code null}.
     *
     * @since TBD
     */
    public List<ArtifactType> extraArtifactTypes() {
        return extraArtifactTypes;
    }

    /**
     * Is session offline?
     */
    public boolean isOffline() {
        return offline;
    }

    /**
     * Snapshot update policy, {@code null} is to use Resolver default.
     */
    public SnapshotUpdatePolicy getSnapshotUpdatePolicy() {
        return snapshotUpdatePolicy;
    }

    /**
     * Checksum policy, {@code null} is to use Resolver default.
     */
    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    /**
     * Whether user {@code settings.xml} should be picked up while configuring Resolver or not.
     */
    public boolean isWithUserSettings() {
        return withUserSettings;
    }

    /**
     * Returns the list of explicitly enabled profile IDs, never {@code null}.
     *
     * @since 2.3.0
     */
    public List<String> getActiveProfileIds() {
        return activeProfileIds;
    }

    /**
     * Returns the list of explicitly disabled profile IDs, never {@code null}.
     *
     * @since 2.3.0
     */
    public List<String> getInactiveProfileIds() {
        return inactiveProfileIds;
    }

    /**
     * Repository listener, {@code null} if none.
     */
    public RepositoryListener getRepositoryListener() {
        return repositoryListener;
    }

    /**
     * Transfer listener, {@code null} if none.
     */
    public TransferListener getTransferListener() {
        return transferListener;
    }

    /**
     * Maven User Home override, of {@code null}.
     *
     * @since 2.4.0
     */
    public Path getMavenUserHomeOverride() {
        return mavenUserHomeOverride;
    }

    /**
     * Maven User Settings override, or {@code null}.
     *
     * @since 2.4.0
     */
    public Path getUserSettingsXmlOverride() {
        return userSettingsXmlOverride;
    }

    /**
     * Maven User settings-security.xml override, or {@code null}.
     *
     * @since 2.4.0
     */
    public Path getUserSettingsSecurityXmlOverride() {
        return userSettingsSecurityXmlOverride;
    }

    /**
     * Maven User Toolchains override, or {@code null}.
     *
     * @since 2.4.0
     */
    public Path getUserToolchainsXmlOverride() {
        return userToolchainsXmlOverride;
    }

    /**
     * Maven Local Repository override, or {@code null}.
     *
     * @since 2.4.0
     */
    public Path getLocalRepositoryOverride() {
        return localRepositoryOverride;
    }

    /**
     * Maven System Home override, of {@code null}.
     *
     * @since 2.4.0
     */
    public Path getMavenSystemHomeOverride() {
        return mavenSystemHomeOverride;
    }

    /**
     * Maven Global Settings override, or {@code null}.
     *
     * @since 2.3.0
     */
    public Path getGlobalSettingsXmlOverride() {
        return globalSettingsXmlOverride;
    }

    /**
     * Maven Global Toolchains override, or {@code null}.
     *
     * @since 2.4.0
     */
    public Path getGlobalToolchainsXmlOverride() {
        return globalToolchainsXmlOverride;
    }

    /**
     * The built, effective settings, or {@code null}.
     *
     * @since 2.3.0
     */
    public Object getEffectiveSettings() {
        return effectiveSettings;
    }

    /**
     * The built, effective setting mixin, or {@code null}.
     *
     * @since 2.4.0
     */
    public Object getEffectiveSettingsMixin() {
        return effectiveSettingsMixin;
    }

    /**
     * Creates {@link Builder} out of current instance.
     *
     * @since 2.4.0
     */
    public Builder toBuilder() {
        return new Builder()
                .withBasedirOverride(basedirOverride)
                .systemProperties(systemProperties)
                .userProperties(userProperties)
                .configProperties(configProperties)
                .repositories(repositories)
                .addRepositoriesOp(addRepositoriesOp)
                .offline(offline)
                .snapshotUpdatePolicy(snapshotUpdatePolicy)
                .checksumPolicy(checksumPolicy)
                .withUserSettings(withUserSettings)
                .withActiveProfileIds(activeProfileIds)
                .withInactiveProfileIds(inactiveProfileIds)
                .repositoryListener(repositoryListener)
                .transferListener(transferListener)
                .withMavenUserHomeOverride(mavenUserHomeOverride)
                .withUserSettingsXmlOverride(userSettingsXmlOverride)
                .withUserSettingsSecurityXmlOverride(userSettingsSecurityXmlOverride)
                .withUserToolchainsXmlOverride(userToolchainsXmlOverride)
                .withLocalRepositoryOverride(localRepositoryOverride)
                .withMavenSystemHomeOverride(mavenSystemHomeOverride)
                .withGlobalSettingsXmlOverride(globalSettingsXmlOverride)
                .withGlobalToolchainsXmlOverride(globalToolchainsXmlOverride)
                .withEffectiveSettings(effectiveSettings)
                .withEffectiveSettingsMixin(effectiveSettingsMixin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContextOverrides that = (ContextOverrides) o;
        return offline == that.offline
                && withUserSettings == that.withUserSettings
                && Objects.equals(basedirOverride, that.basedirOverride)
                && Objects.equals(systemProperties, that.systemProperties)
                && Objects.equals(userProperties, that.userProperties)
                && Objects.equals(configProperties, that.configProperties)
                && Objects.equals(repositories, that.repositories)
                && addRepositoriesOp == that.addRepositoriesOp
                && snapshotUpdatePolicy == that.snapshotUpdatePolicy
                && checksumPolicy == that.checksumPolicy
                && Objects.equals(activeProfileIds, that.activeProfileIds)
                && Objects.equals(inactiveProfileIds, that.inactiveProfileIds)
                && Objects.equals(repositoryListener, that.repositoryListener)
                && Objects.equals(transferListener, that.transferListener)
                && Objects.equals(mavenUserHomeOverride, that.mavenUserHomeOverride)
                && Objects.equals(userSettingsXmlOverride, that.userSettingsXmlOverride)
                && Objects.equals(userSettingsSecurityXmlOverride, that.userSettingsSecurityXmlOverride)
                && Objects.equals(userToolchainsXmlOverride, that.userToolchainsXmlOverride)
                && Objects.equals(localRepositoryOverride, that.localRepositoryOverride)
                && Objects.equals(mavenSystemHomeOverride, that.mavenSystemHomeOverride)
                && Objects.equals(globalSettingsXmlOverride, that.globalSettingsXmlOverride)
                && Objects.equals(globalToolchainsXmlOverride, that.globalToolchainsXmlOverride)
                && Objects.equals(effectiveSettings, that.effectiveSettings)
                && Objects.equals(effectiveSettingsMixin, that.effectiveSettingsMixin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                basedirOverride,
                systemProperties,
                userProperties,
                configProperties,
                repositories,
                addRepositoriesOp,
                offline,
                snapshotUpdatePolicy,
                checksumPolicy,
                withUserSettings,
                activeProfileIds,
                inactiveProfileIds,
                repositoryListener,
                transferListener,
                mavenUserHomeOverride,
                userSettingsXmlOverride,
                userSettingsSecurityXmlOverride,
                userToolchainsXmlOverride,
                localRepositoryOverride,
                mavenSystemHomeOverride,
                globalSettingsXmlOverride,
                globalToolchainsXmlOverride,
                effectiveSettings,
                effectiveSettingsMixin);
    }

    /**
     * Creates a "default" builder instance (that will NOT discover {@code settings.xml}).
     * <p>
     * Note: if you want to obey "Maven environment", you must invoke {@link Builder#withUserSettings(boolean)} with
     * {@code true} parameter at least.
     */
    public static Builder create() {
        return new Builder();
    }

    public static final class Builder {
        private Path basedirOverride = null;

        private Map<String, String> systemProperties = Collections.emptyMap();

        private Map<String, String> userProperties = Collections.emptyMap();

        private Map<String, Object> configProperties = Collections.emptyMap();

        private List<RemoteRepository> repositories = Collections.singletonList(CENTRAL);

        private AddRepositoriesOp addRepositoriesOp = AddRepositoriesOp.PREPEND;

        private List<ArtifactType> extraArtifactTypes = Collections.emptyList();

        private boolean offline = false;

        private SnapshotUpdatePolicy snapshotUpdatePolicy = null;

        private ChecksumPolicy checksumPolicy = null;

        private boolean withUserSettings = false;

        private List<String> activeProfileIds = Collections.emptyList();

        private List<String> inactiveProfileIds = Collections.emptyList();

        private RepositoryListener repositoryListener = null;

        private TransferListener transferListener = null;

        private Path mavenUserHomeOverride = null;

        private Path userSettingsXmlOverride = null;

        private Path userSettingsSecurityXmlOverride = null;

        private Path userToolchainsXmlOverride = null;

        private Path localRepositoryOverride = null;

        private Path mavenSystemHomeOverride = null;

        private Path globalSettingsXmlOverride = null;

        private Path globalToolchainsXmlOverride = null;

        private Object effectiveSettings = null;

        private Object effectiveSettingsMixin = null;

        /**
         * Hide ctor, use {@link #create()} to create new builder instances.
         */
        private Builder() {
            // hidden
        }

        /**
         * Overrides basedir path (cwd), it must be non-{@code null} and point to an existing directory. If these
         * are not met, this method will throw. Basedir by default is initialized with {@code CWD} that is
         * "current working directory" of the process.
         *
         * @since 2.3.0
         */
        public Builder withBasedirOverride(Path basedirOverride) {
            if (basedirOverride != null) {
                if (!Files.isDirectory(basedirOverride)) {
                    throw new IllegalArgumentException("basedir must be existing directory: " + basedirOverride);
                }
            }
            this.basedirOverride = basedirOverride;
            return this;
        }

        /**
         * Sets Maven System Properties to be used. Users usually don't want to tamper with this.
         */
        public Builder systemProperties(Map<String, String> systemProperties) {
            if (systemProperties != null) {
                this.systemProperties = new HashMap<>(systemProperties);
            } else {
                this.systemProperties = Collections.emptyMap();
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
                this.userProperties = Collections.emptyMap();
            }
            return this;
        }

        /**
         * Sets Maven Configuration Properties to be used. These accept {@link Object} values, and may be used for
         * advanced configuration of some Resolver aspect. Usually users don't want to tamper with these.
         */
        public Builder configProperties(Map<String, Object> configProperties) {
            if (configProperties != null) {
                this.configProperties = new HashMap<>(configProperties);
            } else {
                this.configProperties = Collections.emptyMap();
            }
            return this;
        }

        /**
         * Sets the list of {@link RemoteRepository} instance you want to use. The list may replace or append the
         * list of repositories coming from Maven, see {@link #addRepositoriesOp()}.
         * <p>
         * If {@link #withUserSettings(boolean)} invoked with {@code true}, the {@code settings.xml} discovered
         * repositories (and many more) will be used to create context. Also, in case when MIMA runs within Maven,
         * the current project repositories will be provided.
         */
        public Builder repositories(List<RemoteRepository> repositories) {
            if (repositories != null) {
                this.repositories = new ArrayList<>(repositories);
            } else {
                this.repositories = Collections.emptyList();
            }
            return this;
        }

        /**
         * How to handle the {@link #repositories(List)} provided list.
         *
         * @since 2.4.0
         */
        public Builder addRepositoriesOp(AddRepositoriesOp addRepositoriesOp) {
            this.addRepositoriesOp = addRepositoriesOp;
            return this;
        }
        /**
         * Sets the list of {@link ArtifactType} instances you want to extend resolver with. The list will append the
         * existing list of types coming from Maven.
         * <p>
         * In case when MIMA runs within Maven, this is ignored.
         *
         * @since TBD
         */
        public Builder extraArtifactTypes(List<ArtifactType> extraArtifactTypes) {
            if (extraArtifactTypes != null) {
                this.extraArtifactTypes = new ArrayList<>(extraArtifactTypes);
            } else {
                this.extraArtifactTypes = Collections.emptyList();
            }
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
         * Enables or disables use of userSettingsXml, used to find out location of local repository,
         * authentication, remote repositories and many more.
         */
        public Builder withUserSettings(boolean withUserSettings) {
            this.withUserSettings = withUserSettings;
            return this;
        }

        /**
         * Sets explicitly activated profile IDs.
         *
         * @since 2.3.0
         */
        public Builder withActiveProfileIds(List<String> activeProfileIds) {
            if (activeProfileIds != null) {
                this.activeProfileIds = new ArrayList<>(activeProfileIds);
            } else {
                this.activeProfileIds = Collections.emptyList();
            }
            return this;
        }

        /**
         * Sets explicitly inactivated profile IDs.
         *
         * @since 2.3.0
         */
        public Builder withInactiveProfileIds(List<String> inactiveProfileIds) {
            if (inactiveProfileIds != null) {
                this.inactiveProfileIds = new ArrayList<>(inactiveProfileIds);
            } else {
                this.inactiveProfileIds = Collections.emptyList();
            }
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
         * Override for Maven User Home.
         *
         * @since 2.4.0
         */
        public Builder withMavenUserHomeOverride(Path mavenUserHomeOverride) {
            this.mavenUserHomeOverride = mavenUserHomeOverride;
            return this;
        }

        /**
         * Overrides Maven User settings.xml location.
         *
         * @since 2.3.0
         */
        public Builder withUserSettingsXmlOverride(Path userSettingsXmlOverride) {
            this.userSettingsXmlOverride = userSettingsXmlOverride;
            return this;
        }

        /**
         * Overrides Maven User settings-security.xml location.
         *
         * @since 2.3.0
         */
        public Builder withUserSettingsSecurityXmlOverride(Path userSettingsSecurityXmlOverride) {
            this.userSettingsSecurityXmlOverride = userSettingsSecurityXmlOverride;
            return this;
        }

        /**
         * Overrides Maven User toolchains.xml location.
         *
         * @since 2.4.0
         */
        public Builder withUserToolchainsXmlOverride(Path userToolchainsXmlOverride) {
            this.userToolchainsXmlOverride = userToolchainsXmlOverride;
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
         * Sets Maven System Home override.
         *
         * @since 2.4.0
         */
        public Builder withMavenSystemHomeOverride(Path mavenSystemHomeOverride) {
            this.mavenSystemHomeOverride = mavenSystemHomeOverride;
            return this;
        }

        /**
         * Overrides Maven Global settings.xml location.
         *
         * @since 2.3.0
         */
        public Builder withGlobalSettingsXmlOverride(Path globalSettingsXmlOverride) {
            this.globalSettingsXmlOverride = globalSettingsXmlOverride;
            return this;
        }

        /**
         * Overrides Maven Global toolchains.xml location.
         *
         * @since 2.4.0
         */
        public Builder withGlobalToolchainsXmlOverride(Path globalToolchainsXmlOverride) {
            this.globalToolchainsXmlOverride = globalToolchainsXmlOverride;
            return this;
        }

        /**
         * Sets Maven Effective Settings. If set, this fully replaces any discovered settings.
         * <p>
         * Important: it must be "effective" (all paths interpolated, resolved, etc.), as this object is accepted
         * as is, there is no any processing applied to it!
         *
         * @since 2.3.0
         */
        public Builder withEffectiveSettings(Object effectiveSettings) {
            this.effectiveSettings = effectiveSettings;
            return this;
        }

        /**
         * Sets Maven Effective Settings mixin. If set, this is merged into effective settings.
         * <p>
         * Important: it must be "effective" (all paths interpolated, resolved, etc.), as this object is accepted
         * as is, there is no any processing applied to it!
         *
         * @since 2.4.0
         */
        public Builder withEffectiveSettingsMixin(Object effectiveSettingsMixin) {
            this.effectiveSettingsMixin = effectiveSettingsMixin;
            return this;
        }

        /**
         * Builds an immutable instance of {@link ContextOverrides} using so far applied settings and configuration.
         */
        public ContextOverrides build() {
            return new ContextOverrides(
                    basedirOverride,
                    systemProperties,
                    userProperties,
                    configProperties,
                    repositories,
                    addRepositoriesOp,
                    extraArtifactTypes,
                    offline,
                    snapshotUpdatePolicy,
                    checksumPolicy,
                    withUserSettings,
                    activeProfileIds,
                    inactiveProfileIds,
                    repositoryListener,
                    transferListener,
                    mavenUserHomeOverride,
                    userSettingsXmlOverride,
                    userSettingsSecurityXmlOverride,
                    userToolchainsXmlOverride,
                    localRepositoryOverride,
                    mavenSystemHomeOverride,
                    globalSettingsXmlOverride,
                    globalToolchainsXmlOverride,
                    effectiveSettings,
                    effectiveSettingsMixin);
        }
    }
}
