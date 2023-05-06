package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.TreeSet;

/**
 * Registry of known {@link Runtime} instances. It orders them by priority. This class is the "entry point" in MIMA to
 * obtain actual {@link Runtime} instance.
 */
public final class Runtimes {
    public static final Runtimes INSTANCE = new Runtimes();

    private final TreeSet<Runtime> runtimes = new TreeSet<>(Comparator.comparing(Runtime::priority));

    private Runtimes() {}

    /**
     * Returns the {@link Runtime} instance with the highest priority out of all registered instances, never
     * {@code null}. The method should be used to obtains runtime instance to work with.
     */
    public synchronized Runtime getRuntime() {
        Runtime result = null;
        if (!runtimes.isEmpty()) {
            result = runtimes.first();
        }
        if (result == null) {
            ServiceLoader<Runtime> loader = ServiceLoader.load(Runtime.class);
            loader.iterator().forEachRemaining(this::registerRuntime);
            if (runtimes.isEmpty()) {
                throw new IllegalStateException("No Runtime implementation found on classpath");
            }
            result = runtimes.first();
        }
        return result;
    }

    /**
     * Returns an unmodifiable snapshot (copy) collection of all registered {@link Runtime} instances.
     */
    public synchronized Collection<Runtime> getRuntimes() {
        TreeSet<Runtime> result = new TreeSet<>(Comparator.comparing(Runtime::priority));
        result.addAll(runtimes);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Registers a {@link Runtime} instance. If instance with same {@link Runtime#name()} was already registered, this
     * method is no-op (first registration wins).
     */
    public synchronized void registerRuntime(Runtime mimaRuntime) {
        requireNonNull(mimaRuntime);
        if (runtimes.stream().map(Runtime::name).noneMatch(n -> n.equals(mimaRuntime.name()))) {
            runtimes.add(mimaRuntime);
        }
    }

    /**
     * Clears all registered instances.
     */
    public synchronized void resetRuntimes() {
        runtimes.clear();
    }
}
