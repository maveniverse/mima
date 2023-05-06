package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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

public abstract class RuntimeSupport implements Runtime {
    private static final String UNKNOWN = "(unknown)";

    private static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    private static final String MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY = "maven.repo.local.tail.ignoreAvailability";

    private final String name;

    private final int priority;

    private final String mavenVersion;

    protected RuntimeSupport(String name, int priority, String mavenVersion) {
        this.name = requireNonNull(name);
        this.priority = priority;
        this.mavenVersion = mavenVersion == null || mavenVersion.trim().isEmpty() ? UNKNOWN : mavenVersion;
    }

    @Override
    public String name() {
        return name;
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
            if (overrides.getSystemProperties() != null) {
                session.setSystemProperties(overrides.getSystemProperties());
            }
            if (overrides.getUserProperties() != null) {
                session.setUserProperties(overrides.getUserProperties());
            }
            if (overrides.getConfigProperties() != null) {
                session.setConfigProperties(overrides.getConfigProperties());
            }
        }

        session.setOffline(overrides.isOffline());

        customizeLocalRepositoryManager(overrides, context.repositorySystem(), session);

        customizeChecksumPolicy(overrides, session);

        customizeSnapshotUpdatePolicy(overrides, session);

        // settings are used only in creation, not customization

        if (overrides.getTransferListener() != null) {
            session.setTransferListener(overrides.getTransferListener());
        }
        if (overrides.getRepositoryListener() != null) {
            session.setRepositoryListener(overrides.getRepositoryListener());
        }

        ArrayList<RemoteRepository> remoteRepositories = new ArrayList<>();
        if (overrides.isAppendRepositories() || overrides.getRepositories() == null) {
            remoteRepositories.addAll(context.remoteRepositories());
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
                context.repositorySystem(),
                session,
                context.repositorySystem().newResolutionRepositories(session, remoteRepositories),
                null); // derived context: close should NOT shut down repositorySystem
    }

    protected void customizeLocalRepositoryManager(
            ContextOverrides overrides, RepositorySystem repositorySystem, DefaultRepositorySystemSession session) {
        if (overrides.getLocalRepository() == null) {
            return;
        }
        Path localRepoPath = session.getLocalRepository().getBasedir().toPath().toAbsolutePath();
        if (overrides.getLocalRepository().toAbsolutePath().equals(localRepoPath)) {
            return;
        }
        newLocalRepositoryManager(overrides.getLocalRepository().toAbsolutePath(), repositorySystem, session);
    }

    protected void newLocalRepositoryManager(
            Path localRepoPath, RepositorySystem repositorySystem, DefaultRepositorySystemSession session) {
        LocalRepository localRepo =
                new LocalRepository(localRepoPath.toAbsolutePath().toString());
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

    protected static String discoverMavenVersion() {
        return discoverArtifactVersion("org.apache.maven", "maven-resolver-provider", null);
    }

    protected static String discoverArtifactVersion(String groupId, String artifactId, String defVal) {
        Map<String, String> mavenInfo = discoverMavenInfo(groupId, artifactId);
        String versionString = mavenInfo.getOrDefault("version", "").trim();
        if (!versionString.startsWith("${")) {
            return versionString;
        }
        return defVal;
    }

    protected static Map<String, String> discoverMavenInfo(String groupId, String artifactId) {
        final String resource = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        final Properties props = new Properties();
        try (InputStream is = RuntimeSupport.class.getResourceAsStream("/" + resource)) {
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
                + name + "', priority="
                + priority + ", mavenVersion="
                + mavenVersion + ", managedRepositorySystem="
                + managedRepositorySystem() + "}";
    }
}
