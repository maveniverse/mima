package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import java.io.Closeable;
import java.util.List;
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
 */
public final class Context implements Closeable {
    private final RuntimeSupport runtime;

    private final RepositorySystem repositorySystem;

    private final RepositorySystemSession repositorySystemSession;

    private final List<RemoteRepository> remoteRepositories;

    private final Runnable managedCloser;

    public Context(
            RuntimeSupport runtime,
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySystemSession,
            List<RemoteRepository> remoteRepositories,
            Runnable managedCloser) {
        this.runtime = requireNonNull(runtime);
        this.repositorySystemSession = requireNonNull(repositorySystemSession);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.remoteRepositories = requireNonNull(remoteRepositories);
        this.managedCloser = managedCloser;
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
     * Returns a new {@link Context} instance, that is customized using passed in {@link ContextOverrides}, using this
     * instance as "base".
     */
    public Context customize(ContextOverrides overrides) {
        return runtime.customizeContext(overrides, this, false);
    }

    /**
     * Closes the context. Once closed context instance should not be used anymore.
     */
    @Override
    public void close() {
        // in the future session may become closeable as well
        // repositorySystemSession.close();
        if (managedCloser != null) {
            managedCloser.run();
        }
    }
}
