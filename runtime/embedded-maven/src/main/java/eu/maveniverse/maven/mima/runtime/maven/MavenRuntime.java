/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.maven;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
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
                discoverArtifactVersion("eu.maveniverse.maven.mima.runtime", "embedded-maven", UNKNOWN),
                10,
                mavenVersion(rt));
        this.repositorySystem = repositorySystem;
        this.plexusContainer = plexusContainer;
        this.mavenSessionProvider = mavenSessionProvider;
        this.runtimeInformation = rt;
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
        return UNKNOWN;
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
        switch (overrides.addRepositoriesOp()) {
            case REPLACE:
                repositories.clear();
                repositories.addAll(overrides.getRepositories());
                break;
            case PREPEND:
                repositories.addAll(0, overrides.getRepositories());
                break;
            case APPEND:
                repositories.addAll(overrides.getRepositories());
                break;
            default:
                throw new IllegalStateException("Unknown overrides op: " + overrides.addRepositoriesOp());
        }

        MavenUserHome mavenUserHome = discoverMavenUserHome(mavenSession.getRequest());
        MavenSystemHome mavenSystemHome = discoverMavenSystemHome(mavenSession.getRequest());
        RepositorySystemSession session = mavenSession.getRepositorySession();

        ContextOverrides.Builder effectiveOverridesBuilder = overrides.toBuilder();
        effectiveOverridesBuilder.withUserSettings(true); // embedded
        effectiveOverridesBuilder.repositories(repositories);
        effectiveOverridesBuilder.systemProperties(session.getSystemProperties());
        effectiveOverridesBuilder.userProperties(session.getUserProperties());
        effectiveOverridesBuilder.configProperties(session.getConfigProperties());
        effectiveOverridesBuilder.withActiveProfileIds(
                mavenSession.getCurrentProject().getInjectedProfileIds().values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
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
                        Collections.unmodifiableList(
                                repositorySystem.newResolutionRepositories(session, effective.getRepositories())),
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
