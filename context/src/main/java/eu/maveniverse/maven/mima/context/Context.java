package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * The MIMA context holds references to {@link RepositorySystem}, {@link RepositorySystemSession} and list of
 * {@link RemoteRepository}. Context is {@link Closeable}, ideally used in try-with-resource constructs.
 * <p>
 * The very first context instance application creates using {@link Runtime#create(ContextOverrides)} is called "root
 * context", and creation of it can be considered "heavy" operation (runtime dependent). Root context should be kept
 * open as long as application is expected to make use of Resolver.
 * <p>
 * Context instances may be customized with {@link #customize(ContextOverrides)} method, in that case the returned
 * context is "derived" from this context. In those cases the context instances should be handled "as nested", so
 * closed in opposite order as they were obtained. Creating customized contexts can be considered "light" operation,
 * as they merely alter the {@link RepositorySystemSession} instance and repositories, while the
 * {@link RepositorySystem} is just inherited from this instance (is not reconstructed).
 *
 * @see Runtimes#getRuntime()
 * @see Runtime#create(ContextOverrides)
 */
public final class Context implements Closeable {
    private final AtomicBoolean closed;
    private final RuntimeSupport runtime;

    private final ContextOverrides contextOverrides;

    private final Path basedir;

    private final MavenUserHome mavenUserHome;

    private final MavenSystemHome mavenSystemHome;

    private final RepositorySystem repositorySystem;

    private final RepositorySystemSession repositorySystemSession;

    private final List<RemoteRepository> remoteRepositories;

    private final HTTPProxy httpProxy;

    private final Runnable managedCloser;

    public Context(
            RuntimeSupport runtime,
            ContextOverrides contextOverrides,
            Path basedir,
            MavenUserHome mavenUserHome,
            MavenSystemHome mavenSystemHome,
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySystemSession,
            List<RemoteRepository> remoteRepositories,
            HTTPProxy httpProxy,
            Runnable managedCloser) {
        this.closed = new AtomicBoolean(false);
        this.runtime = requireNonNull(runtime);
        this.contextOverrides = requireNonNull(contextOverrides);
        this.basedir = requireNonNull(basedir);
        this.mavenUserHome = requireNonNull(mavenUserHome);
        this.mavenSystemHome = mavenSystemHome;
        this.repositorySystemSession = requireNonNull(repositorySystemSession);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.remoteRepositories = requireNonNull(remoteRepositories);
        this.httpProxy = httpProxy;
        this.managedCloser = managedCloser;
    }

    /**
     * Returns the effective {@link ContextOverrides}, never {@code null}. This instance MAY be different from the user
     * supplied one to {@link Runtime#create(ContextOverrides)}, as it will contain discovered configuration as well.
     *
     * @since 2.1.0
     */
    public ContextOverrides contextOverrides() {
        return contextOverrides;
    }

    /**
     * The basedir ("cwd"), never {@code null}.
     *
     * @since 2.3.0
     */
    public Path basedir() {
        return basedir;
    }

    /**
     * Returns effective {@link MavenUserHome}, never {@code null}.
     *
     * @since 2.1.0
     */
    public MavenUserHome mavenUserHome() {
        return mavenUserHome;
    }

    /**
     * Returns effective {@link MavenSystemHome}, may be {@code null}, if no Maven Home discovered.
     *
     * @since 2.1.0
     */
    public MavenSystemHome mavenSystemHome() {
        return mavenSystemHome;
    }

    /**
     * Returns the {@link RepositorySystemSession}, never {@code null}.
     */
    public RepositorySystemSession repositorySystemSession() {
        return repositorySystemSession;
    }

    /**
     * Returns the {@link RepositorySystem}, never {@code null}.
     */
    public RepositorySystem repositorySystem() {
        return repositorySystem;
    }

    /**
     * Returns the list of {@link RemoteRepository}, never {@code null}.
     */
    public List<RemoteRepository> remoteRepositories() {
        return remoteRepositories;
    }

    /**
     * Returns HTTP Proxy that Resolver will use, or {@code null}. This configuration may come from user
     * {@code settings.xml}.
     *
     * @since 2.4.0
     */
    public HTTPProxy httpProxy() {
        return httpProxy;
    }

    /**
     * Returns a new {@link Context} instance, that is customized using passed in {@link ContextOverrides}, using this
     * instance as "base".
     */
    public Context customize(ContextOverrides overrides) {
        if (closed.get()) {
            throw new IllegalStateException("context is closed");
        }
        return runtime.customizeContext(overrides, this, false);
    }

    /**
     * Closes the context. Once closed context instance should not be used anymore.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // in the future session may become closeable as well
            // repositorySystemSession.close();
            if (managedCloser != null) {
                managedCloser.run();
            }
        }
    }
}
