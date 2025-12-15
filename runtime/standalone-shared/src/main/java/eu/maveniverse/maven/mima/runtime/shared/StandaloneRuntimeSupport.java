/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.shared;

import static java.util.stream.Collectors.toMap;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.HTTPProxy;
import eu.maveniverse.maven.mima.context.Lookup;
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.internal.MavenSystemHomeImpl;
import eu.maveniverse.maven.mima.context.internal.MavenUserHomeImpl;
import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.ActivationOS;
import org.apache.maven.settings.ActivationProperty;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.TrackableBase;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.settings.merge.MavenSettingsMerger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
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
        super(name, discoverVersion(), priority, discoverMavenVersion(), discoverResolverVersion());
    }

    private static String discoverVersion() {
        Map<String, String> version = loadClasspathProperties(
                StandaloneRuntimeSupport.class,
                "/eu/maveniverse/maven/mima/runtime/shared/internal/version.properties");
        String result = version.get("version");
        if (result == null || result.trim().isEmpty() || result.startsWith("${")) {
            return UNKNOWN_VERSION;
        }
        return result;
    }

    protected PreBoot preBoot(ContextOverrides overrides) {
        Map<String, String> systemProperties = defaultSystemProperties();
        systemProperties.putAll(overrides.getSystemProperties());
        Map<String, String> userProperties = new HashMap<>(overrides.getUserProperties());
        Map<String, Object> configProperties = new HashMap<>(systemProperties);
        configProperties.putAll(userProperties);
        configProperties.putAll(overrides.getConfigProperties());

        Path localRepositoryOverride = safeAbsolute(overrides.getLocalRepositoryOverride());
        if (localRepositoryOverride == null) {
            String localRepoPath = (String) configProperties.get("maven.repo.local");
            if (localRepoPath != null) {
                localRepositoryOverride = Paths.get(localRepoPath).toAbsolutePath();
            }
        }

        Path mavenSystemHome = safeAbsolute(overrides.getMavenSystemHomeOverride());
        if (mavenSystemHome == null) {
            String mavenHome = (String) configProperties.get("maven.home");
            if (mavenHome == null) {
                mavenHome = (String) configProperties.get("env.MAVEN_HOME");
            }
            if (mavenHome != null) {
                mavenSystemHome = Paths.get(mavenHome).toAbsolutePath();
            }
        }

        ContextOverrides alteredOverrides = overrides.toBuilder()
                .systemProperties(systemProperties)
                .userProperties(userProperties)
                .configProperties(configProperties)
                .withLocalRepositoryOverride(localRepositoryOverride)
                .build();

        MavenUserHomeImpl mavenUserHomeImpl = defaultMavenUserHome().derive(alteredOverrides);
        MavenSystemHomeImpl mavenSystemHomeImpl =
                mavenSystemHome != null ? new MavenSystemHomeImpl(mavenSystemHome).derive(alteredOverrides) : null;
        Path baseDir =
                alteredOverrides.getBasedirOverride() != null ? alteredOverrides.getBasedirOverride() : DEFAULT_BASEDIR;

        return new PreBoot(alteredOverrides, mavenUserHomeImpl, mavenSystemHomeImpl, baseDir);
    }

    protected Context buildContext(
            StandaloneRuntimeSupport runtime,
            PreBoot preBoot,
            RepositorySystem repositorySystem,
            SettingsBuilder settingsBuilder,
            SettingsDecrypter settingsDecrypter,
            ProfileSelector profileSelector,
            Lookup lookup,
            Runnable managedCloser) {
        try {
            ContextOverrides alteredOverrides = preBoot.getOverrides();
            MavenUserHomeImpl mavenUserHomeImpl = preBoot.getMavenUserHome();
            MavenSystemHomeImpl mavenSystemHomeImpl = preBoot.getMavenSystemHome();
            Path baseDir = preBoot.getBaseDir();

            Settings settings =
                    newEffectiveSettings(alteredOverrides, mavenUserHomeImpl, mavenSystemHomeImpl, settingsBuilder);

            // settings: local repository
            if (settings.getLocalRepository() != null && alteredOverrides.getLocalRepositoryOverride() == null) {
                mavenUserHomeImpl = mavenUserHomeImpl.withLocalRepository(
                        Paths.get(settings.getLocalRepository()).toAbsolutePath());
            }

            // settings: active proxy
            Proxy proxy = settings.getActiveProxy();
            HTTPProxy httpProxy = null;
            if (proxy != null) {
                httpProxy = toHTTPProxy(proxy);
            }

            // settings: active profiles
            List<Profile> activeProfiles =
                    activeProfilesByActivation(alteredOverrides, baseDir, settings, profileSelector);
            if (!activeProfiles.isEmpty()) {
                alteredOverrides = alteredOverrides.toBuilder()
                        .withActiveProfileIds(
                                activeProfiles.stream().map(Profile::getId).collect(Collectors.toList()))
                        .build();
            }

            // settings: active profile properties
            // In MIMA there is no "project context", but to support Resolver configuration
            // via settings.xml (MNG-7590) we push them into user properties.
            // Note: putIfAbsent used, as if key is present, it means it was added via override already
            HashMap<String, String> profileProperties = new HashMap<>();
            for (Profile profile : activeProfiles) {
                profile.getProperties()
                        .stringPropertyNames()
                        .forEach(n ->
                                profileProperties.put(n, profile.getProperties().getProperty(n)));
            }
            if (!profileProperties.isEmpty()) {
                Map<String, String> profileUserProperties = new HashMap<>(alteredOverrides.getUserProperties());
                profileProperties.forEach(profileUserProperties::putIfAbsent);
                alteredOverrides = alteredOverrides.toBuilder()
                        .userProperties(profileUserProperties)
                        .build();
            }

            DefaultRepositorySystemSession session = newRepositorySession(
                    alteredOverrides, mavenUserHomeImpl, repositorySystem, settings, settingsDecrypter);

            // settings: active profile repositories (if enabled), strictly preserve order
            final LinkedHashMap<String, RemoteRepository> remoteRepositories = new LinkedHashMap<>();
            if (alteredOverrides.addRepositoriesOp() != ContextOverrides.AddRepositoriesOp.REPLACE) {
                if (alteredOverrides.addRepositoriesOp() == ContextOverrides.AddRepositoriesOp.PREPEND) {
                    alteredOverrides.getRepositories().forEach(r -> remoteRepositories.put(r.getId(), r));
                }
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
                        RemoteRepository remoteRepository = builder.build();
                        remoteRepositories.put(remoteRepository.getId(), remoteRepository);
                    }
                }
                if (alteredOverrides.addRepositoriesOp() == ContextOverrides.AddRepositoriesOp.APPEND) {
                    alteredOverrides.getRepositories().forEach(r -> remoteRepositories.put(r.getId(), r));
                }
            } else {
                alteredOverrides.getRepositories().forEach(r -> remoteRepositories.put(r.getId(), r));
            }
            alteredOverrides = alteredOverrides.toBuilder()
                    .repositories(new ArrayList<>(remoteRepositories.values()))
                    .build();
            return new Context(
                    runtime,
                    alteredOverrides,
                    baseDir,
                    mavenUserHomeImpl,
                    mavenSystemHomeImpl,
                    repositorySystem,
                    session,
                    httpProxy,
                    lookup,
                    managedCloser);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create context from scratch", e);
        }
    }

    private HTTPProxy toHTTPProxy(Proxy proxy) {
        HashMap<String, Object> data = new HashMap<>();
        if (proxy.getUsername() != null) {
            data.put("username", proxy.getUsername());
        }
        if (proxy.getPassword() != null) {
            data.put("password", proxy.getPassword());
        }
        return new HTTPProxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getNonProxyHosts(), data);
    }

    protected Settings newEffectiveSettings(
            ContextOverrides overrides,
            MavenUserHome mavenUserHome,
            MavenSystemHome mavenSystemHome,
            SettingsBuilder settingsBuilder)
            throws SettingsBuildingException {
        if (!overrides.isWithUserSettings()) {
            return new Settings();
        }
        if (overrides.getEffectiveSettings() instanceof Settings) {
            return (Settings) overrides.getEffectiveSettings();
        }
        DefaultSettingsBuildingRequest settingsBuilderRequest = new DefaultSettingsBuildingRequest();

        Properties systemProperties = new Properties();
        systemProperties.putAll(overrides.getSystemProperties());
        settingsBuilderRequest.setSystemProperties(systemProperties);
        Properties userProperties = new Properties();
        userProperties.putAll(overrides.getUserProperties());
        settingsBuilderRequest.setUserProperties(userProperties);
        if (mavenSystemHome != null) {
            settingsBuilderRequest.setGlobalSettingsFile(
                    mavenSystemHome.settingsXml().toFile());
        }
        settingsBuilderRequest.setUserSettingsFile(mavenUserHome.settingsXml().toFile());
        Settings result = settingsBuilder.build(settingsBuilderRequest).getEffectiveSettings();
        if (overrides.getEffectiveSettingsMixin() instanceof Settings) {
            settingsMixin(result, (Settings) overrides.getEffectiveSettingsMixin());
        }
        return result;
    }

    protected void settingsMixin(Settings settings, Settings mixin) {
        // Special care for mixin that is about to add Proxy:
        // - disable all other proxies, if exists
        // Hence:
        // - if mixin proxy is active, it prevails
        // - if mixin proxy is inactive, shuts down proxy
        boolean mixinAddsProxy = !mixin.getProxies().isEmpty();
        if (mixinAddsProxy) {
            for (Proxy proxy : settings.getProxies()) {
                proxy.setActive(false);
            }
            settings.flushActiveProxy();
        }
        new MavenSettingsMerger().merge(settings, mixin, TrackableBase.GLOBAL_LEVEL);
    }

    protected List<Profile> activeProfilesByActivation(
            ContextOverrides overrides, Path basedir, Settings settings, ProfileSelector profileSelector) {
        if (profileSelector == null) {
            return activeProfiles(settings);
        } else {
            DefaultProfileActivationContext context = new DefaultProfileActivationContext();
            context.setProjectDirectory(basedir.toFile());
            context.setActiveProfileIds(
                    Stream.concat(settings.getActiveProfiles().stream(), overrides.getActiveProfileIds().stream())
                            .distinct()
                            .collect(Collectors.toList()));
            context.setInactiveProfileIds(overrides.getInactiveProfileIds());
            context.setSystemProperties(overrides.getSystemProperties());
            context.setUserProperties(overrides.getUserProperties());
            ModelProblemCollector collector = new ModelProblemCollector() {
                @Override
                public void add(ModelProblemCollectorRequest req) {}
            };
            return profileSelector
                    .getActiveProfiles(
                            settings.getProfiles().stream()
                                    .map(StandaloneRuntimeSupport::convertFromSettingsProfile)
                                    .collect(Collectors.toList()),
                            context,
                            collector)
                    .stream()
                    .map(StandaloneRuntimeSupport::convertToSettingsProfile)
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
            MavenUserHome mavenUserHome,
            RepositorySystem repositorySystem,
            Settings settings,
            SettingsDecrypter settingsDecrypter) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        if (!overrides.extraArtifactTypes().isEmpty()) {
            DefaultArtifactTypeRegistry registry = (DefaultArtifactTypeRegistry) session.getArtifactTypeRegistry();
            overrides.extraArtifactTypes().forEach(registry::add);
        }

        session.setCache(new DefaultRepositoryCache());

        LinkedHashMap<Object, Object> configProps = new LinkedHashMap<>(overrides.getConfigProperties());
        configProps.putIfAbsent(ConfigurationProperties.USER_AGENT, getUserAgent());

        // internal things, these should not be overridden
        configProps.put(ConfigurationProperties.INTERACTIVE, false);
        configProps.put("maven.startTime", new Date());

        overrides.isOffline().ifPresent(session::setOffline);

        overrides.isIgnoreArtifactDescriptorRepositories().ifPresent(session::setIgnoreArtifactDescriptorRepositories);

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

                // stash config entries just like Maven does
                configProps.put("aether.connector.wagon.config." + server.getId(), dom);

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

        newLocalRepositoryManager(mavenUserHome.localRepository(), repositorySystem, session);

        return session;
    }

    protected String getUserAgent() {
        return "Apache-Maven/" + mavenVersion() + " (Java " + System.getProperty("java.version") + "; "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + "; MIMA " + version() + ")";
    }

    /**
     * Collects (Maven) system properties as Maven does: it is a mixture of {@link System#getenv()} prefixed with
     * {@code "env."} and Java System properties.
     */
    protected static Map<String, String> defaultSystemProperties() {
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
    protected static Path safeAbsolute(Path path) {
        if (path == null) {
            return null;
        }
        return path.toAbsolutePath();
    }

    // SettingsUtils copy BEGIN
    // Below are helper methods copied from Maven 3.9.2 SettingsUtils class, stripped unused methods and inlined
    // Original class
    // https://github.com/apache/maven/blob/maven-3.9.2/maven-core/src/main/java/org/apache/maven/settings/SettingsUtils.java
    // Note: all methods accessors modified to protected, to expose them to downstream runtimes, if needed.
    // Currently, these methods are used in this class only.

    protected static Profile convertToSettingsProfile(org.apache.maven.model.Profile modelProfile) {
        Profile profile = new Profile();
        profile.setId(modelProfile.getId());
        org.apache.maven.model.Activation modelActivation = modelProfile.getActivation();
        if (modelActivation != null) {
            Activation activation = new Activation();
            activation.setActiveByDefault(modelActivation.isActiveByDefault());
            activation.setJdk(modelActivation.getJdk());
            org.apache.maven.model.ActivationProperty modelProp = modelActivation.getProperty();
            if (modelProp != null) {
                ActivationProperty prop = new ActivationProperty();
                prop.setName(modelProp.getName());
                prop.setValue(modelProp.getValue());
                activation.setProperty(prop);
            }
            org.apache.maven.model.ActivationOS modelOs = modelActivation.getOs();
            if (modelOs != null) {
                ActivationOS os = new ActivationOS();
                os.setArch(modelOs.getArch());
                os.setFamily(modelOs.getFamily());
                os.setName(modelOs.getName());
                os.setVersion(modelOs.getVersion());
                activation.setOs(os);
            }
            ActivationFile modelFile = modelActivation.getFile();
            if (modelFile != null) {
                org.apache.maven.settings.ActivationFile file = new org.apache.maven.settings.ActivationFile();
                file.setExists(modelFile.getExists());
                file.setMissing(modelFile.getMissing());
                activation.setFile(file);
            }
            profile.setActivation(activation);
        }
        profile.setProperties(modelProfile.getProperties());
        List<org.apache.maven.model.Repository> repos = modelProfile.getRepositories();
        if (repos != null) {
            for (org.apache.maven.model.Repository repo : repos) {
                profile.addRepository(convertToSettingsRepository(repo));
            }
        }
        List<org.apache.maven.model.Repository> pluginRepos = modelProfile.getPluginRepositories();
        if (pluginRepos != null) {
            for (org.apache.maven.model.Repository pluginRepo : pluginRepos) {
                profile.addPluginRepository(convertToSettingsRepository(pluginRepo));
            }
        }
        return profile;
    }

    protected static org.apache.maven.model.Profile convertFromSettingsProfile(Profile settingsProfile) {
        org.apache.maven.model.Profile profile = new org.apache.maven.model.Profile();
        profile.setId(settingsProfile.getId());
        profile.setSource("settings.xml");
        Activation settingsActivation = settingsProfile.getActivation();
        if (settingsActivation != null) {
            org.apache.maven.model.Activation activation = new org.apache.maven.model.Activation();
            activation.setActiveByDefault(settingsActivation.isActiveByDefault());
            activation.setJdk(settingsActivation.getJdk());
            ActivationProperty settingsProp = settingsActivation.getProperty();
            if (settingsProp != null) {
                org.apache.maven.model.ActivationProperty prop = new org.apache.maven.model.ActivationProperty();
                prop.setName(settingsProp.getName());
                prop.setValue(settingsProp.getValue());
                activation.setProperty(prop);
            }
            ActivationOS settingsOs = settingsActivation.getOs();
            if (settingsOs != null) {
                org.apache.maven.model.ActivationOS os = new org.apache.maven.model.ActivationOS();
                os.setArch(settingsOs.getArch());
                os.setFamily(settingsOs.getFamily());
                os.setName(settingsOs.getName());
                os.setVersion(settingsOs.getVersion());
                activation.setOs(os);
            }
            org.apache.maven.settings.ActivationFile settingsFile = settingsActivation.getFile();
            if (settingsFile != null) {
                ActivationFile file = new ActivationFile();
                file.setExists(settingsFile.getExists());
                file.setMissing(settingsFile.getMissing());
                activation.setFile(file);
            }
            profile.setActivation(activation);
        }
        profile.setProperties(settingsProfile.getProperties());
        List<Repository> repos = settingsProfile.getRepositories();
        if (repos != null) {
            for (Repository repo : repos) {
                profile.addRepository(convertFromSettingsRepository(repo));
            }
        }
        List<Repository> pluginRepos = settingsProfile.getPluginRepositories();
        if (pluginRepos != null) {
            for (Repository pluginRepo : pluginRepos) {
                profile.addPluginRepository(convertFromSettingsRepository(pluginRepo));
            }
        }
        return profile;
    }

    protected static org.apache.maven.model.Repository convertFromSettingsRepository(Repository settingsRepo) {
        org.apache.maven.model.Repository repo = new org.apache.maven.model.Repository();
        repo.setId(settingsRepo.getId());
        repo.setLayout(settingsRepo.getLayout());
        repo.setName(settingsRepo.getName());
        repo.setUrl(settingsRepo.getUrl());
        if (settingsRepo.getSnapshots() != null) {
            repo.setSnapshots(convertFromSettingsRepositoryPolicy(settingsRepo.getSnapshots()));
        }
        if (settingsRepo.getReleases() != null) {
            repo.setReleases(convertFromSettingsRepositoryPolicy(settingsRepo.getReleases()));
        }
        return repo;
    }

    protected static org.apache.maven.model.RepositoryPolicy convertFromSettingsRepositoryPolicy(
            org.apache.maven.settings.RepositoryPolicy settingsPolicy) {
        org.apache.maven.model.RepositoryPolicy policy = new org.apache.maven.model.RepositoryPolicy();
        policy.setEnabled(settingsPolicy.isEnabled());
        policy.setUpdatePolicy(settingsPolicy.getUpdatePolicy());
        policy.setChecksumPolicy(settingsPolicy.getChecksumPolicy());
        return policy;
    }

    protected static Repository convertToSettingsRepository(org.apache.maven.model.Repository modelRepo) {
        Repository repo = new Repository();
        repo.setId(modelRepo.getId());
        repo.setLayout(modelRepo.getLayout());
        repo.setName(modelRepo.getName());
        repo.setUrl(modelRepo.getUrl());
        if (modelRepo.getSnapshots() != null) {
            repo.setSnapshots(convertToSettingsRepositoryPolicy(modelRepo.getSnapshots()));
        }
        if (modelRepo.getReleases() != null) {
            repo.setReleases(convertToSettingsRepositoryPolicy(modelRepo.getReleases()));
        }
        return repo;
    }

    protected static org.apache.maven.settings.RepositoryPolicy convertToSettingsRepositoryPolicy(
            org.apache.maven.model.RepositoryPolicy modelPolicy) {
        org.apache.maven.settings.RepositoryPolicy policy = new org.apache.maven.settings.RepositoryPolicy();
        policy.setEnabled(modelPolicy.isEnabled());
        policy.setUpdatePolicy(modelPolicy.getUpdatePolicy());
        policy.setChecksumPolicy(modelPolicy.getChecksumPolicy());
        return policy;
    }

    // SettingsUtils copy END
}
