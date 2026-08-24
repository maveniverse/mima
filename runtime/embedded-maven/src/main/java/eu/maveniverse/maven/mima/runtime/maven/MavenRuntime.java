/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.maven;

import static java.util.stream.Collectors.toList;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.HTTPProxy;
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.internal.MavenSystemHomeImpl;
import eu.maveniverse.maven.mima.context.internal.MavenUserHomeImpl;
import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import eu.maveniverse.maven.mima.runtime.maven.internal.PlexusLookup;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.DefaultSessionData;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.eclipse.sisu.Nullable;

@Singleton
@Named
public final class MavenRuntime extends RuntimeSupport {
    private final RepositorySystem repositorySystem;

    private final PlexusContainer plexusContainer;

    private final Provider<MavenSession> mavenSessionProvider;

    private final RuntimeInformation runtimeInformation;

    @Inject
    public MavenRuntime(
            @Nullable RepositorySystem repositorySystem,
            @Nullable PlexusContainer plexusContainer,
            Provider<MavenSession> mavenSessionProvider,
            @Nullable RuntimeInformation rt) {
        super(
                "embedded-maven",
                discoverArtifactVersion(
                        MavenRuntime.class, "eu.maveniverse.maven.mima.runtime", "embedded-maven", UNKNOWN_VERSION),
                10,
                mavenVersion(rt),
                "embedded");
        // when embedded in Maven, classloading isolation does not allow us to discover Resolver version
        this.repositorySystem = repositorySystem;
        this.plexusContainer = plexusContainer;
        this.mavenSessionProvider = mavenSessionProvider;
        this.runtimeInformation = rt;
    }

    @Override
    protected Context customizeContext(
            RuntimeSupport runtime, ContextOverrides overrides, Context context, boolean reset) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        if (reset) {
            session.setCache(new DefaultRepositoryCache());
            session.setData(new DefaultSessionData());
        }

        if (managedRepositorySystem()) {
            session.setSystemProperties(overrides.getSystemProperties());
            session.setUserProperties(overrides.getUserProperties());
            session.setConfigProperties(overrides.getConfigProperties());
        }

        overrides.isOffline().ifPresent(session::setOffline);

        overrides.isIgnoreArtifactDescriptorRepositories().ifPresent(session::setIgnoreArtifactDescriptorRepositories);

        customizeLocalRepositoryManager(context, session);

        customizeChecksumPolicy(overrides, session);

        customizeArtifactDescriptorPolicy(overrides, session);

        customizeSnapshotUpdatePolicy(overrides, session);

        // settings are used only in creation, not customization

        if (overrides.getTransferListener() != null) {
            session.setTransferListener(overrides.getTransferListener());
        }
        if (overrides.getRepositoryListener() != null) {
            session.setRepositoryListener(overrides.getRepositoryListener());
        }

        session.setReadOnly();

        overrides = overrides.toBuilder()
                .repositories(customizeRemoteRepositories(overrides, context.remoteRepositories()))
                .build();

        return new Context(
                runtime,
                overrides,
                overrides.getBasedirOverride() != null ? overrides.getBasedirOverride() : context.basedir(),
                ((MavenUserHomeImpl) context.mavenUserHome()).derive(overrides),
                context.mavenSystemHome() != null
                        ? ((MavenSystemHomeImpl) context.mavenSystemHome()).derive(overrides)
                        : null,
                context.repositorySystem(),
                session,
                context.httpProxy(),
                context.lookup(),
                null); // derived context: close should NOT shut down repositorySystem
    }

    private void customizeLocalRepositoryManager(Context context, DefaultRepositorySystemSession session) {
        Path localRepoPath = session.getLocalRepository().getBasedir().toPath();
        if (context.mavenUserHome().localRepository().equals(localRepoPath)) {
            return;
        }
        newLocalRepositoryManager(context.mavenUserHome().localRepository(), context.repositorySystem(), session);
    }

    private void newLocalRepositoryManager(
            Path localRepoPath, RepositorySystem repositorySystem, DefaultRepositorySystemSession session) {
        LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
        LocalRepositoryManager lrm = repositorySystem.newLocalRepositoryManager(session, localRepo);

        String localRepoTail = ConfigUtils.getString(session, null, MAVEN_REPO_LOCAL_TAIL);
        if (localRepoTail != null) {
            ArrayList<LocalRepositoryManager> tail = new ArrayList<>();
            List<String> paths = Arrays.stream(localRepoTail.split(","))
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .collect(toList());
            for (String path : paths) {
                tail.add(repositorySystem.newLocalRepositoryManager(session, new LocalRepository(path)));
            }
            session.setLocalRepositoryManager(new ChainedLocalRepositoryManager(lrm, tail, true));
        } else {
            session.setLocalRepositoryManager(lrm);
        }
    }

    private void customizeChecksumPolicy(ContextOverrides overrides, DefaultRepositorySystemSession session) {
        if (overrides.getChecksumPolicy() != null) {
            switch (overrides.getChecksumPolicy()) {
                case FAIL:
                    session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
                    break;
                case WARN:
                    session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
                    break;
                case IGNORE:
                    session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
                    break;
            }
        }
    }

    private void customizeArtifactDescriptorPolicy(ContextOverrides overrides, DefaultRepositorySystemSession session) {
        if (overrides.getArtifactDescriptorPolicy() != null) {
            session.setArtifactDescriptorPolicy(overrides.getArtifactDescriptorPolicy());
        }
    }

    private void customizeSnapshotUpdatePolicy(ContextOverrides overrides, DefaultRepositorySystemSession session) {
        if (overrides.getArtifactUpdatePolicy() != null) {
            switch (overrides.getArtifactUpdatePolicy()) {
                case ALWAYS:
                    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
                    break;
                case NEVER:
                    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
                    break;
            }
        }
        if (overrides.getMetadataUpdatePolicy() != null) {
            switch (overrides.getMetadataUpdatePolicy()) {
                case ALWAYS:
                    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
                    break;
                case NEVER:
                    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
                    break;
            }
        }
    }

    private List<RemoteRepository> customizeRemoteRepositories(
            ContextOverrides contextOverrides, List<RemoteRepository> remoteRepositories) {
        if (Objects.equals(contextOverrides.getRepositories(), remoteRepositories)) {
            // no change here
            return remoteRepositories;
        }
        ArrayList<RemoteRepository> result = new ArrayList<>();
        if (contextOverrides.addRepositoriesOp() == ContextOverrides.AddRepositoriesOp.REPLACE) {
            result.addAll(contextOverrides.getRepositories());
        } else {
            if (contextOverrides.addRepositoriesOp() == ContextOverrides.AddRepositoriesOp.PREPEND) {
                result.addAll(contextOverrides.getRepositories());
            }
            result.addAll(remoteRepositories);
            if (contextOverrides.addRepositoriesOp() == ContextOverrides.AddRepositoriesOp.APPEND) {
                result.addAll(contextOverrides.getRepositories());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public boolean isReady() {
        return repositorySystem != null && plexusContainer != null && runtimeInformation != null;
    }

    private static String mavenVersion(RuntimeInformation runtimeInformation) {
        if (runtimeInformation != null) {
            String mavenVersion = runtimeInformation.getMavenVersion();
            if (mavenVersion != null && !mavenVersion.trim().isEmpty()) {
                return mavenVersion;
            }
        }
        return UNKNOWN_VERSION;
    }

    @Override
    public boolean managedRepositorySystem() {
        return false;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        MavenSession mavenSession = mavenSessionProvider.get();
        boolean projectPresent = mavenSession.getRequest().isProjectPresent();

        Path basedir;
        if (projectPresent) {
            basedir = mavenSession.getCurrentProject().getBasedir().toPath().toAbsolutePath();
        } else {
            basedir = DEFAULT_BASEDIR;
        }

        List<RemoteRepository> repositories =
                new ArrayList<>(mavenSession.getCurrentProject().getRemoteProjectRepositories());
        if (overrides.getRepositories() != ContextOverrides.DEFAULT_REMOTE_REPOSITORIES) {
            List<RemoteRepository> overrideRepositories = repositorySystem.newResolutionRepositories(
                    mavenSession.getRepositorySession(), overrides.getRepositories());
            switch (overrides.addRepositoriesOp()) {
                case REPLACE:
                    repositories.clear();
                    repositories.addAll(overrideRepositories);
                    break;
                case PREPEND:
                    repositories.addAll(0, overrideRepositories);
                    break;
                case APPEND:
                    repositories.addAll(overrideRepositories);
                    break;
                default:
                    throw new IllegalStateException("Unknown overrides op: " + overrides.addRepositoriesOp());
            }
        }

        MavenUserHome mavenUserHome = discoverMavenUserHome(mavenSession.getRequest());
        MavenSystemHome mavenSystemHome = discoverMavenSystemHome(mavenSession.getRequest());
        RepositorySystemSession session = mavenSession.getRepositorySession();

        ContextOverrides.Builder effectiveOverridesBuilder = overrides.toBuilder();
        effectiveOverridesBuilder.keepBareRepositories(true); // embedded; maven handles them
        effectiveOverridesBuilder.withUserSettings(true); // embedded
        effectiveOverridesBuilder.repositories(repositories);
        effectiveOverridesBuilder.systemProperties(session.getSystemProperties());
        effectiveOverridesBuilder.userProperties(session.getUserProperties());
        effectiveOverridesBuilder.configProperties(session.getConfigProperties());
        effectiveOverridesBuilder.withActiveProfileIds(
                mavenSession.getCurrentProject().getInjectedProfileIds().values().stream()
                        .flatMap(Collection::stream)
                        .collect(toList()));
        effectiveOverridesBuilder.withInactiveProfileIds(
                mavenSession.getRequest().getInactiveProfiles());

        ContextOverrides effective = effectiveOverridesBuilder.build();
        return customizeContext(
                this,
                effective,
                new Context(
                        this,
                        effective,
                        basedir,
                        mavenUserHome,
                        mavenSystemHome,
                        repositorySystem,
                        session,
                        toHTTPProxy(mavenSession.getSettings().getActiveProxy()),
                        new PlexusLookup(plexusContainer),
                        null),
                false); // unmanaged context: close should NOT shut down repositorySystem
    }

    private HTTPProxy toHTTPProxy(Proxy proxy) {
        if (proxy == null) {
            return null;
        }

        HashMap<String, Object> data = new HashMap<>();
        if (proxy.getUsername() != null) {
            data.put("username", proxy.getUsername());
        }
        if (proxy.getPassword() != null) {
            data.put("password", proxy.getPassword());
        }
        return new HTTPProxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getNonProxyHosts(), data);
    }

    private MavenUserHome discoverMavenUserHome(MavenExecutionRequest executionRequest) {
        return new MavenUserHomeImpl(
                DEFAULT_MAVEN_USER_HOME,
                executionRequest.getUserSettingsFile() != null
                        ? executionRequest.getUserSettingsFile().toPath().toAbsolutePath()
                        : null,
                null,
                executionRequest.getUserToolchainsFile() != null
                        ? executionRequest.getUserToolchainsFile().toPath().toAbsolutePath()
                        : null,
                executionRequest.getLocalRepositoryPath().toPath().toAbsolutePath());
    }

    private MavenSystemHome discoverMavenSystemHome(MavenExecutionRequest executionRequest) {
        return new MavenSystemHomeImpl(
                Paths.get(System.getProperty("maven.home")).toAbsolutePath(),
                executionRequest.getGlobalSettingsFile() != null
                        ? executionRequest.getGlobalSettingsFile().toPath().toAbsolutePath()
                        : null,
                executionRequest.getGlobalToolchainsFile() != null
                        ? executionRequest.getGlobalToolchainsFile().toPath().toAbsolutePath()
                        : null);
    }
}
