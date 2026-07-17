/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context.internal;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Support class for {@link Runtime} implementations.
 */
public abstract class RuntimeSupport implements Runtime {
    protected static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    public static final Path DEFAULT_BASEDIR =
            Paths.get(System.getProperty("user.dir")).toAbsolutePath();

    public static final Path DEFAULT_USER_HOME =
            Paths.get(System.getProperty("user.home")).toAbsolutePath();

    public static final Path DEFAULT_MAVEN_USER_HOME = DEFAULT_USER_HOME.resolve(".m2");

    private final String name;

    private final String version;

    private final int priority;

    private final String mavenVersion;

    private final String resolverVersion;

    protected RuntimeSupport(String name, String version, int priority, String mavenVersion, String resolverVersion) {
        this.name = requireNonNull(name);
        this.version = requireNonNull(version);
        this.priority = priority;
        this.mavenVersion = requireNonNull(mavenVersion);
        this.resolverVersion = requireNonNull(resolverVersion);
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

    @Override
    public String mavenVersion() {
        return mavenVersion;
    }

    @Override
    public String resolverVersion() {
        return resolverVersion;
    }

    @Override
    public abstract boolean managedRepositorySystem();

    public Context customizeContext(ContextOverrides overrides, Context context, boolean reset) {
        return customizeContext(this, overrides, context, reset);
    }

    protected abstract Context customizeContext(
            RuntimeSupport runtime, ContextOverrides overrides, Context context, boolean reset);

    /**
     * Visible for test.
     */
    public MavenUserHomeImpl defaultMavenUserHome() {
        return new MavenUserHomeImpl(DEFAULT_MAVEN_USER_HOME);
    }

    protected static String discoverMavenVersion() {
        return discoverArtifactVersion(
                RuntimeSupport.class, "org.apache.maven", "maven-resolver-provider", UNKNOWN_VERSION);
    }

    protected static String discoverResolverVersion() {
        return discoverArtifactVersion(
                RuntimeSupport.class, "org.apache.maven.resolver", "maven-resolver-api", UNKNOWN_VERSION);
    }

    protected static String discoverArtifactVersion(Class<?> clazz, String groupId, String artifactId, String defVal) {
        Map<String, String> mavenPomProperties = loadPomProperties(clazz, groupId, artifactId);
        String versionString = mavenPomProperties.getOrDefault("version", "").trim();
        if (!versionString.isEmpty() && !versionString.startsWith("${")) {
            return versionString;
        }
        return defVal;
    }

    protected static Map<String, String> loadPomProperties(Class<?> clazz, String groupId, String artifactId) {
        return loadClasspathProperties(clazz, "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
    }

    protected static Map<String, String> loadClasspathProperties(Class<?> clazz, String resource) {
        final Properties props = new Properties();
        try (InputStream is = clazz.getResourceAsStream(resource)) {
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
                + mavenVersion + ", resolverVersion="
                + resolverVersion + ", managedRepositorySystem="
                + managedRepositorySystem() + "}";
    }
}
