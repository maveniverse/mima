package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    protected static Context customizeContext(ContextOverrides overrides, Context context, boolean reset) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        if (reset) {
            session.setCache(new DefaultRepositoryCache());
            session.setData(new DefaultSessionData());
        }

        if (overrides.getUserProperties() != null) {
            session.getConfigProperties().putAll(overrides.getUserProperties());
            session.setUserProperties(overrides.getUserProperties());
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
                context.isManagedRepositorySystem(),
                context.repositorySystem(),
                session,
                overrides.getRepositories() != null ? overrides.getRepositories() : context.remoteRepositories());
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='" + name + '\'' + ", priority=" + priority + "}";
    }
}
