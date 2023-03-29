package org.cstamas.maven.mima.core.context;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.repository.RemoteRepository;

public final class MimaContextOverrides {
    private final Map<String, String> userProperties;

    private final List<RemoteRepository> repositories;

    private final boolean offline;

    private final Path localRepository;

    private final boolean forceUpdate;

    private final boolean withUserSettings;

    private final List<Path> settingsXml;

    private final RepositoryListener repositoryListener;

    private MimaContextOverrides(Builder builder) {
        this.userProperties = builder.userProperties;
        this.repositories = builder.repositories;
        this.offline = builder.offline;
        this.localRepository = builder.localRepository;
        this.forceUpdate = builder.forceUpdate;
        this.withUserSettings = builder.withUserSettings;
        this.settingsXml = builder.settingsXml;
        this.repositoryListener = builder.repositoryListener;
    }

    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public boolean isOffline() {
        return offline;
    }

    public Path getLocalRepository() {
        return localRepository;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public boolean isWithUserSettings() {
        return withUserSettings;
    }

    public List<Path> getSettingsXml() {
        return settingsXml;
    }

    public RepositoryListener getRepositoryListener() {
        return repositoryListener;
    }

    public static final class Builder {
        private Map<String, String> userProperties;

        private List<RemoteRepository> repositories;

        private boolean offline;

        private Path localRepository;

        private boolean forceUpdate;

        private boolean withUserSettings;

        private List<Path> settingsXml;

        private RepositoryListener repositoryListener;

        public static Builder create() {
            return new Builder();
        }

        public Builder userProperties(Map<String, String> userProperties) {
            requireNonNull(userProperties);
            this.userProperties = new HashMap<>(userProperties);
            return this;
        }

        public Builder addUserProperty(String name, String value) {
            requireNonNull(name);
            if (this.userProperties == null) {
                this.userProperties = new HashMap<>();
            }
            this.userProperties.put(name, value);
            return this;
        }

        public Builder repositories(List<RemoteRepository> repositories) {
            requireNonNull(repositories);
            this.repositories = new ArrayList<>(repositories);
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

        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder localRepository(Path localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        public Builder forceUpdate(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
            return this;
        }

        public Builder withUserSettings(boolean withUserSettings) {
            this.withUserSettings = withUserSettings;
            return this;
        }

        public Builder settingsXml(Path settingsXml) {
            requireNonNull(settingsXml);
            if (this.settingsXml == null) {
                this.settingsXml = new ArrayList<>();
            }
            this.settingsXml.add(settingsXml);
            return this;
        }

        public Builder settingsXml(List<Path> settingsXml) {
            requireNonNull(settingsXml);
            this.settingsXml = new ArrayList<>(settingsXml);
            return this;
        }

        public Builder repositoryListener(RepositoryListener repositoryListener) {
            this.repositoryListener = repositoryListener;
            return this;
        }

        public MimaContextOverrides build() {
            return new MimaContextOverrides(this);
        }
    }
}
