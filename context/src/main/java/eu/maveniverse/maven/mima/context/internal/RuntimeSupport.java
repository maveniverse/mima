/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context.internal;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.DefaultSessionData;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;

/**
 * Support class for {@link Runtime} implementations.
 */
public abstract class RuntimeSupport implements Runtime {
    public static final String UNKNOWN = "(unknown)";

    private static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    private static final String MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY = "maven.repo.local.tail.ignoreAvailability";

    public static final Path DEFAULT_BASEDIR =
            Paths.get(System.getProperty("user.dir")).toAbsolutePath();

    public static final Path DEFAULT_USER_HOME =
            Paths.get(System.getProperty("user.home")).toAbsolutePath();

    public static final Path DEFAULT_MAVEN_USER_HOME = DEFAULT_USER_HOME.resolve(".m2");

    private final String name;

    private final String version;

    private final int priority;

    private final String mavenVersion;

    protected RuntimeSupport(String name, String version, int priority, String mavenVersion) {
        this.name = requireNonNull(name);
        this.version = requireNonNull(version);
        this.priority = priority;
        this.mavenVersion = requireNonNull(mavenVersion);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public int priority() {
        return priority;
    }

    public String mavenVersion() {
        return mavenVersion;
    }

    @Override
    public abstract boolean managedRepositorySystem();

    public Context customizeContext(ContextOverrides overrides, Context context, boolean reset) {
        return customizeContext(this, overrides, context, reset);
    }

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

        session.setOffline(overrides.isOffline());

        customizeLocalRepositoryManager(context, session);

        customizeChecksumPolicy(overrides, session);

        customizeSnapshotUpdatePolicy(overrides, session);

        // settings are used only in creation, not customization

        if (overrides.getTransferListener() != null) {
            session.setTransferListener(overrides.getTransferListener());
        }
        if (overrides.getRepositoryListener() != null) {
            session.setRepositoryListener(overrides.getRepositoryListener());
        }
        List<RemoteRepository> remoteRepositories =
                customizeRemoteRepositories(overrides, context.remoteRepositories());

        session.setReadOnly();

        return new Context(
                runtime,
                overrides,
                overrides.getBasedirOverride() != null ? overrides.getBasedirOverride() : context.basedir(),
                ((MavenUserHomeImpl) context.mavenUserHome()).derive(overrides),
                ((MavenSystemHomeImpl) context.mavenSystemHome()).derive(overrides),
                context.repositorySystem(),
                session,
                context.repositorySystem().newResolutionRepositories(session, remoteRepositories),
                context.httpProxy(),
                null); // derived context: close should NOT shut down repositorySystem
    }

    protected void customizeLocalRepositoryManager(Context context, DefaultRepositorySystemSession session) {
        Path localRepoPath = session.getLocalRepository().getBasedir().toPath();
        if (context.mavenUserHome().localRepository().equals(localRepoPath)) {
            return;
        }
        newLocalRepositoryManager(context.mavenUserHome().localRepository(), context.repositorySystem(), session);
    }

    protected void newLocalRepositoryManager(
            Path localRepoPath, RepositorySystem repositorySystem, DefaultRepositorySystemSession session) {
        LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
        LocalRepositoryManager lrm = repositorySystem.newLocalRepositoryManager(session, localRepo);

        String localRepoTail = ConfigUtils.getString(session, null, MAVEN_REPO_LOCAL_TAIL);
        if (localRepoTail != null) {
            boolean ignoreTailAvailability =
                    ConfigUtils.getBoolean(session, true, MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY);
            ArrayList<LocalRepositoryManager> tail = new ArrayList<>();
            List<String> paths = Arrays.stream(localRepoTail.split(","))
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .collect(toList());
            for (String path : paths) {
                tail.add(repositorySystem.newLocalRepositoryManager(session, new LocalRepository(path)));
            }
            session.setLocalRepositoryManager(new ChainedLocalRepositoryManager(lrm, tail, ignoreTailAvailability));
        } else {
            session.setLocalRepositoryManager(lrm);
        }
    }

    protected void customizeChecksumPolicy(ContextOverrides overrides, DefaultRepositorySystemSession session) {
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

    protected void customizeSnapshotUpdatePolicy(ContextOverrides overrides, DefaultRepositorySystemSession session) {
        if (overrides.getSnapshotUpdatePolicy() != null) {
            switch (overrides.getSnapshotUpdatePolicy()) {
                case ALWAYS:
                    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
                    break;
                case NEVER:
                    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
                    break;
            }
        }
    }

    protected List<RemoteRepository> customizeRemoteRepositories(
            ContextOverrides contextOverrides, List<RemoteRepository> remoteRepositories) {
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
        return result;
    }

    protected MavenUserHomeImpl defaultMavenUserHome() {
        return new MavenUserHomeImpl(DEFAULT_MAVEN_USER_HOME);
    }

    protected static String discoverMavenVersion() {
        return discoverArtifactVersion("org.apache.maven", "maven-resolver-provider", UNKNOWN);
    }

    protected static String discoverArtifactVersion(String groupId, String artifactId, String defVal) {
        Map<String, String> mavenPomProperties = loadPomProperties(groupId, artifactId);
        String versionString = mavenPomProperties.getOrDefault("version", "").trim();
        if (!versionString.startsWith("${")) {
            return versionString;
        }
        return defVal;
    }

    protected static Map<String, String> loadPomProperties(String groupId, String artifactId) {
        return loadClasspathProperties("/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
    }

    protected static Map<String, String> loadClasspathProperties(String resource) {
        final Properties props = new Properties();
        try (InputStream is = RuntimeSupport.class.getResourceAsStream(resource)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // fall through
        }
        return props.entrySet().stream()
                .collect(toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next,
                        HashMap::new));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='"
                + name + "', version="
                + version + ", priority="
                + priority + ", mavenVersion="
                + mavenVersion + ", managedRepositorySystem="
                + managedRepositorySystem() + "}";
    }
}
