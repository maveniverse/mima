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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Proxy;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named
public final class MavenRuntime extends RuntimeSupport {
    private final RepositorySystem repositorySystem;

    private final Provider<MavenSession> mavenSessionProvider;

    @Inject
    public MavenRuntime(
            RepositorySystem repositorySystem, Provider<MavenSession> mavenSessionProvider, RuntimeInformation rt) {
        super(
                "embedded-maven",
                discoverArtifactVersion("eu.maveniverse.maven.mima.runtime", "embedded-maven", UNKNOWN),
                10,
                mavenVersion(rt));
        this.repositorySystem = repositorySystem;
        this.mavenSessionProvider = mavenSessionProvider;
    }

    private static String mavenVersion(RuntimeInformation runtimeInformation) {
        String mavenVersion = runtimeInformation.getMavenVersion();
        if (mavenVersion == null || mavenVersion.trim().isEmpty()) {
            return UNKNOWN;
        }
        return mavenVersion;
    }

    @Override
    public boolean managedRepositorySystem() {
        return false;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        MavenSession mavenSession = mavenSessionProvider.get();

        MavenProject currentProject = mavenSession.getCurrentProject();
        Path basedir =
                currentProject != null ? currentProject.getBasedir().toPath().toAbsolutePath() : DEFAULT_BASEDIR;
        MavenUserHome mavenUserHome = discoverMavenUserHome(mavenSession.getRequest());
        MavenSystemHome mavenSystemHome = discoverMavenSystemHome(mavenSession.getRequest());
        RepositorySystemSession session = mavenSession.getRepositorySession();

        ContextOverrides.Builder effectiveOverridesBuilder = overrides.toBuilder();
        effectiveOverridesBuilder.withUserSettings(true); // embedded
        effectiveOverridesBuilder.systemProperties(session.getSystemProperties());
        effectiveOverridesBuilder.userProperties(session.getUserProperties());
        effectiveOverridesBuilder.configProperties(session.getConfigProperties());

        ContextOverrides effective = effectiveOverridesBuilder.build();
        return customizeContext(
                this,
                overrides,
                new Context(
                        this,
                        effective,
                        basedir,
                        mavenUserHome,
                        mavenSystemHome,
                        repositorySystem,
                        session,
                        repositorySystem.newResolutionRepositories(session, effective.getRepositories()),
                        toHTTPProxy(mavenSession.getSettings().getActiveProxy()),
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
