package eu.maveniverse.maven.mima.runtime.shared;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import eu.maveniverse.maven.mima.runtime.shared.internal.SettingsUtils;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
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
import org.apache.maven.settings.building.SettingsProblem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StandaloneRuntimeSupport extends RuntimeSupport {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected StandaloneRuntimeSupport(String name, int priority) {
        super(name, priority, discoverMavenVersion());
    }

    protected Context buildContext(
            StandaloneRuntimeSupport runtime,
            ContextOverrides overrides,
            RepositorySystem repositorySystem,
            SettingsBuilder settingsBuilder,
            SettingsDecrypter settingsDecrypter,
            ProfileSelector profileSelector,
            Runnable managedCloser) {
        try {
            Settings settings = newEffectiveSettings(overrides, settingsBuilder);
            DefaultRepositorySystemSession session =
                    newRepositorySession(overrides, repositorySystem, settings, settingsDecrypter);
            ArrayList<RemoteRepository> remoteRepositories = new ArrayList<>();
            if (overrides.isAppendRepositories() || overrides.getRepositories().isEmpty()) {
                remoteRepositories.add(ContextOverrides.CENTRAL);
                List<Profile> activeProfiles = activeProfilesByActivation(overrides, settings, profileSelector);
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
            if (!overrides.getRepositories().isEmpty()) {
                if (overrides.isAppendRepositories()) {
                    remoteRepositories.addAll(overrides.getRepositories());
                } else {
                    remoteRepositories = new ArrayList<>(overrides.getRepositories());
                }
            }
            return new Context(
                    runtime,
                    overrides,
                    repositorySystem,
                    session,
                    repositorySystem.newResolutionRepositories(session, remoteRepositories),
                    managedCloser);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create context from scratch", e);
        }
    }

    protected Settings newEffectiveSettings(ContextOverrides overrides, SettingsBuilder settingsBuilder)
            throws SettingsBuildingException {
        if (!overrides.isWithUserSettings()) {
            return new Settings();
        }
        if (overrides.getEffectiveSettings() != null) {
            return (Settings) overrides.getEffectiveSettings();
        }
        DefaultSettingsBuildingRequest settingsBuilderRequest = new DefaultSettingsBuildingRequest();

        Properties systemProperties = new Properties();
        systemProperties.putAll(overrides.getSystemProperties());
        settingsBuilderRequest.setSystemProperties(systemProperties);
        Properties userProperties = new Properties();
        userProperties.putAll(overrides.getUserProperties());
        settingsBuilderRequest.setUserProperties(userProperties);
        if (overrides.getGlobalSettingsXmlOverride() != null) {
            settingsBuilderRequest.setGlobalSettingsFile(
                    overrides.getGlobalSettingsXmlOverride().toFile());
        } else if (overrides.getMavenSystemHome() != null) {
            settingsBuilderRequest.setGlobalSettingsFile(
                    overrides.getMavenSystemHome().settingsXml().toFile());
        }
        settingsBuilderRequest.setUserSettingsFile(
                overrides.getMavenUserHome().settingsXml().toFile());
        return settingsBuilder.build(settingsBuilderRequest).getEffectiveSettings();
    }

    protected List<Profile> activeProfilesByActivation(
            ContextOverrides overrides, Settings settings, ProfileSelector profileSelector) {
        if (profileSelector == null) {
            return activeProfiles(settings);
        } else {
            DefaultProfileActivationContext context = new DefaultProfileActivationContext();
            context.setProjectDirectory(
                    Paths.get(System.getProperty("user.dir")).toFile());
            context.setActiveProfileIds(settings.getActiveProfiles());
            context.setSystemProperties(overrides.getSystemProperties());
            context.setUserProperties(overrides.getUserProperties());
            ModelProblemCollector collector = new ModelProblemCollector() {
                @Override
                public void add(ModelProblemCollectorRequest req) {}
            };
            return profileSelector
                    .getActiveProfiles(
                            settings.getProfiles().stream()
                                    .map(SettingsUtils::convertFromSettingsProfile)
                                    .collect(Collectors.toList()),
                            context,
                            collector)
                    .stream()
                    .map(SettingsUtils::convertToSettingsProfile)
                    .collect(Collectors.toList());
        }
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
            ContextOverrides overrides,
            RepositorySystem repositorySystem,
            Settings settings,
            SettingsDecrypter settingsDecrypter) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setCache(new DefaultRepositoryCache());

        LinkedHashMap<Object, Object> configProps = new LinkedHashMap<>(overrides.getConfigProperties());
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());

        // First add properties populated from settings.xml
        List<Profile> activeProfiles = activeProfiles(settings);
        for (Profile profile : activeProfiles) {
            configProps.putAll(profile.getProperties());
        }

        // internal things, these should not be overridden
        configProps.put(ConfigurationProperties.INTERACTIVE, false);
        configProps.put("maven.startTime", new Date());

        session.setOffline(overrides.isOffline());

        customizeChecksumPolicy(overrides, session);

        customizeSnapshotUpdatePolicy(overrides, session);

        // we should not interfere with "real Maven"
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(false, false));

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(settings.getProxies());
        decrypt.setServers(settings.getServers());
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt(decrypt);

        if (logger.isDebugEnabled()) {
            for (SettingsProblem problem : decrypted.getProblems()) {
                logger.debug(problem.getMessage(), problem.getException());
            }
        }

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
        for (Proxy proxy : decrypted.getProxies()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            proxySelector.add(
                    new org.eclipse.aether.repository.Proxy(
                            proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                    proxy.getNonProxyHosts());
        }
        session.setProxySelector(proxySelector);

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : decrypted.getServers()) {
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

        session.setSystemProperties(overrides.getSystemProperties());
        session.setUserProperties(overrides.getUserProperties());
        session.setConfigProperties(configProps);

        if (overrides.getTransferListener() != null) {
            session.setTransferListener(overrides.getTransferListener());
        }
        if (overrides.getRepositoryListener() != null) {
            session.setRepositoryListener(overrides.getRepositoryListener());
        }

        newLocalRepositoryManager(overrides.getMavenUserHome().localRepository(), repositorySystem, session);

        return session;
    }

    protected String getUserAgent() {
        return "Apache-Maven/" + mavenVersion() + " (Java " + System.getProperty("java.version") + "; "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
    }
}
