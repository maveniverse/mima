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
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;

public abstract class RuntimeSupport implements Runtime {
    protected static final String MAVEN_REPO_LOCAL = "maven.repo.local";

    private static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    private static final String MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY = "maven.repo.local.tail.ignoreAvailability";

    private final String name;

    private final int priority;

    protected RuntimeSupport(String name, int priority) {
        this.name = requireNonNull(name);
        this.priority = priority;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public abstract boolean managedRepositorySystem();

    public Context customizeContext(ContextOverrides overrides, Context context, boolean reset) {
        return customizeContext(this, overrides, context, reset);
    }

    protected static Context customizeContext(
            RuntimeSupport runtime, ContextOverrides overrides, Context context, boolean reset) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        if (reset) {
            session.setCache(new DefaultRepositoryCache());
            session.setData(new DefaultSessionData());
        }

        if (overrides.getUserProperties() != null) {
            session.getConfigProperties().putAll(overrides.getUserProperties());
            session.setUserProperties(overrides.getUserProperties());
        }
        if (overrides.getConfigProperties() != null) {
            session.getConfigProperties().putAll(overrides.getConfigProperties());
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

        return new Context(
                runtime,
                context.repositorySystem(),
                session,
                overrides.getRepositories() != null
                        ? context.repositorySystem().newResolutionRepositories(session, overrides.getRepositories())
                        : context.remoteRepositories(),
                null); // derived context: close should NOT shut down repositorySystem
    }

    protected static void customizeLocalRepositoryManager(
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

    protected static void newLocalRepositoryManager(
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

    protected static void customizeChecksumPolicy(ContextOverrides overrides, DefaultRepositorySystemSession session) {
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

    protected static void customizeSnapshotUpdatePolicy(
            ContextOverrides overrides, DefaultRepositorySystemSession session) {
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

    protected static RuntimeVersions discoverVersions() {
        return new RuntimeVersions(
                discoverMavenInfoVersion("org.apache.maven.resolver", "maven-resolver-api", "n/a"),
                discoverMavenInfoVersion("org.apache.maven", "maven-resolver-provider", "n/a"));
    }

    protected static String discoverMavenInfoVersion(String groupId, String artifactId, String defVal) {
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
        RuntimeVersions rt = runtimeVersions();
        return getClass().getSimpleName() + "{name='"
                + name + '\'' + ", priority="
                + priority + ", mavenVersion="
                + rt.mavenVersion() + ", resolverVersion="
                + rt.resolverVersion() + '}';
    }
}
