package eu.maveniverse.maven.mima.runtime.shared;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.RuntimeSupport;
import eu.maveniverse.maven.mima.context.RuntimeVersions;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

public abstract class StandaloneRuntimeSupport extends RuntimeSupport {
    protected final RuntimeVersions runtimeVersions;

    protected StandaloneRuntimeSupport(String name, int priority) {
        super(name, priority);
        this.runtimeVersions = discoverVersions();
    }

    @Override
    public RuntimeVersions runtimeVersions() {
        return runtimeVersions;
    }

    protected Context buildContext(
            StandaloneRuntimeSupport runtime,
            ContextOverrides overrides,
            RepositorySystem repositorySystem,
            SettingsBuilder settingsBuilder,
            SettingsDecrypter settingsDecrypter,
            Runnable managedCloser) {
        try {
            Settings settings = newEffectiveSettings(overrides, settingsBuilder, settingsDecrypter);
            DefaultRepositorySystemSession session = newRepositorySession(overrides, repositorySystem, settings);
            ArrayList<RemoteRepository> remoteRepositories = new ArrayList<>();
            if (overrides.isAppendRepositories() || overrides.getRepositories() == null) {
                remoteRepositories.add(ContextOverrides.CENTRAL);
                List<Profile> activeProfiles = activeProfiles(settings);
                for (Profile profile : activeProfiles) {
                    for (Repository repository : profile.getRepositories()) {
                        RemoteRepository.Builder builder = new RemoteRepository.Builder(
                                repository.getId(), repository.getLayout(), repository.getUrl());
                        if (repository.getReleases() != null) {
                            builder.setReleasePolicy(new RepositoryPolicy(
                                    repository.getReleases().isEnabled(),
                                    RepositoryPolicy.UPDATE_POLICY_DAILY,
                                    RepositoryPolicy.CHECKSUM_POLICY_WARN));
                        } else {
                            builder.setReleasePolicy(new RepositoryPolicy());
                        }
                        if (repository.getSnapshots() != null) {
                            builder.setSnapshotPolicy(new RepositoryPolicy(
                                    repository.getSnapshots().isEnabled(),
                                    RepositoryPolicy.UPDATE_POLICY_DAILY,
                                    RepositoryPolicy.CHECKSUM_POLICY_WARN));
                        } else {
                            builder.setSnapshotPolicy(new RepositoryPolicy(false, null, null));
                        }
                        remoteRepositories.add(builder.build());
                    }
                }
            }
            if (overrides.getRepositories() != null) {
                if (overrides.isAppendRepositories()) {
                    remoteRepositories.addAll(overrides.getRepositories());
                } else {
                    remoteRepositories = new ArrayList<>(overrides.getRepositories());
                }
            }
            return new Context(
                    runtime,
                    repositorySystem,
                    session,
                    repositorySystem.newResolutionRepositories(session, remoteRepositories),
                    managedCloser);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create context from scratch", e);
        }
    }

    protected Settings newEffectiveSettings(
            ContextOverrides overrides, SettingsBuilder settingsBuilder, SettingsDecrypter settingsDecrypter)
            throws SettingsBuildingException {
        DefaultSettingsBuildingRequest settingsBuilderRequest = new DefaultSettingsBuildingRequest();
        settingsBuilderRequest.setSystemProperties(System.getProperties());
        if (overrides.getUserProperties() != null) {
            settingsBuilderRequest.getUserProperties().putAll(overrides.getUserProperties());
        }

        if (overrides.isWithUserSettings()) {
            if (overrides.getSettingsXml() != null) {
                settingsBuilderRequest.setUserSettingsFile(
                        overrides.getSettingsXml().toFile());
            } else {
                settingsBuilderRequest.setUserSettingsFile(ContextOverrides.USER_SETTINGS_XML.toFile());
            }
        }
        Settings effectiveSettings =
                settingsBuilder.build(settingsBuilderRequest).getEffectiveSettings();

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(effectiveSettings.getProxies());
        decrypt.setServers(effectiveSettings.getServers());
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt(decrypt);

        if (!decrypted.getProblems().isEmpty()) {
            throw new SettingsBuildingException(decrypted.getProblems());
        }

        return effectiveSettings;
    }

    protected List<Profile> activeProfiles(Settings settings) {
        HashMap<String, Profile> result = new HashMap<>();
        Map<String, Profile> profileMap = settings.getProfilesAsMap();
        // explicitly activated: settings/activeProfiles
        for (String profileId : settings.getActiveProfiles()) {
            Profile profile = profileMap.get(profileId);
            if (profile != null) {
                result.put(profile.getId(), profile);
            }
        }
        // implicitly activated: currently only activeByDefault
        for (Profile profile : settings.getProfiles()) {
            Activation activation = profile.getActivation();
            if (activation != null) {
                if (activation.isActiveByDefault()) {
                    result.put(profile.getId(), profile);
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    protected DefaultRepositorySystemSession newRepositorySession(
            ContextOverrides overrides, RepositorySystem repositorySystem, Settings settings) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setCache(new DefaultRepositoryCache());

        LinkedHashMap<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, false);
        configProps.put("maven.startTime", new Date());
        // First add properties populated from settings.xml
        List<Profile> activeProfiles = activeProfiles(settings);
        for (Profile profile : activeProfiles) {
            configProps.putAll(profile.getProperties());
        }
        // Resolver's ConfigUtils solely rely on config properties, that is why we need to add both here as well.
        configProps.putAll(System.getProperties());
        if (overrides.getUserProperties() != null) {
            configProps.putAll(overrides.getUserProperties());
        }
        if (overrides.getConfigProperties() != null) {
            configProps.putAll(overrides.getConfigProperties());
        }

        session.setOffline(overrides.isOffline());

        customizeChecksumPolicy(overrides, session);

        customizeSnapshotUpdatePolicy(overrides, session);

        // we should not interfere with "real Maven"
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(false, false));

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : settings.getMirrors()) {
            mirrorSelector.add(
                    mirror.getId(),
                    mirror.getUrl(),
                    mirror.getLayout(),
                    false,
                    mirror.isBlocked(),
                    mirror.getMirrorOf(),
                    mirror.getMirrorOfLayouts());
        }
        session.setMirrorSelector(mirrorSelector);

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (Proxy proxy : settings.getProxies()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            proxySelector.add(
                    new org.eclipse.aether.repository.Proxy(
                            proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                    proxy.getNonProxyHosts());
        }
        session.setProxySelector(proxySelector);

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : settings.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());

            if (server.getConfiguration() != null) {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for (int i = dom.getChildCount() - 1; i >= 0; i--) {
                    Xpp3Dom child = dom.getChild(i);
                    if ("wagonProvider".equals(child.getName())) {
                        dom.removeChild(i);
                    }
                }

                // Translate to proper resolver configuration properties as well (as Plexus XML above is Wagon specific
                // only), but support only configuration/httpConfiguration/all, see
                // https://maven.apache.org/guides/mini/guide-http-settings.html
                Map<String, String> headers = null;
                Integer connectTimeout = null;
                Integer requestTimeout = null;

                Xpp3Dom httpHeaders = dom.getChild("httpHeaders");
                if (httpHeaders != null) {
                    Xpp3Dom[] properties = httpHeaders.getChildren("property");
                    if (properties != null && properties.length > 0) {
                        headers = new HashMap<>();
                        for (Xpp3Dom property : properties) {
                            headers.put(
                                    property.getChild("name").getValue(),
                                    property.getChild("value").getValue());
                        }
                    }
                }

                Xpp3Dom connectTimeoutXml = dom.getChild("connectTimeout");
                if (connectTimeoutXml != null) {
                    connectTimeout = Integer.parseInt(connectTimeoutXml.getValue());
                }

                Xpp3Dom requestTimeoutXml = dom.getChild("requestTimeout");
                if (requestTimeoutXml != null) {
                    requestTimeout = Integer.parseInt(requestTimeoutXml.getValue());
                }

                // org.eclipse.aether.ConfigurationProperties.HTTP_HEADERS => Map<String, String>
                if (headers != null) {
                    configProps.put(ConfigurationProperties.HTTP_HEADERS + "." + server.getId(), headers);
                }
                // org.eclipse.aether.ConfigurationProperties.CONNECT_TIMEOUT => int
                if (connectTimeout != null) {
                    configProps.put(ConfigurationProperties.CONNECT_TIMEOUT + "." + server.getId(), connectTimeout);
                }
                // org.eclipse.aether.ConfigurationProperties.REQUEST_TIMEOUT => int
                if (requestTimeout != null) {
                    configProps.put(ConfigurationProperties.REQUEST_TIMEOUT + "." + server.getId(), requestTimeout);
                }
            }

            configProps.put("aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
        session.setAuthenticationSelector(authSelector);

        session.setUserProperties(
                overrides.getUserProperties() != null ? overrides.getUserProperties() : new HashMap<>());
        session.setSystemProperties(System.getProperties());
        session.setConfigProperties(configProps);

        if (overrides.getTransferListener() != null) {
            session.setTransferListener(overrides.getTransferListener());
        }
        if (overrides.getRepositoryListener() != null) {
            session.setRepositoryListener(overrides.getRepositoryListener());
        }

        Path localRepoPath;
        if (overrides.getLocalRepository() != null) {
            localRepoPath = overrides.getLocalRepository();
        } else if (settings.getLocalRepository() != null) {
            localRepoPath = Paths.get(settings.getLocalRepository());
        } else {
            localRepoPath = ContextOverrides.USER_LOCAL_REPOSITORY;
        }
        newLocalRepositoryManager(localRepoPath, repositorySystem, session);

        return session;
    }

    protected String getUserAgent() {
        String version = discoverVersions().mavenVersion();
        return "Apache-Maven/" + version + " (Java " + System.getProperty("java.version") + "; "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
    }
}
