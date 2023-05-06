package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.TreeSet;

public final class Runtimes {
    public static final Runtimes INSTANCE = new Runtimes();

    private final TreeSet<Runtime> runtimes = new TreeSet<>(Comparator.comparing(Runtime::priority));

    private Runtimes() {}

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

    public synchronized Collection<Runtime> getRuntimes() {
        TreeSet<Runtime> result = new TreeSet<>(Comparator.comparing(Runtime::priority));
        result.addAll(runtimes);
        return Collections.unmodifiableSet(result);
    }

    public synchronized void registerRuntime(Runtime mimaRuntime) {
        requireNonNull(mimaRuntime);
        if (runtimes.stream().map(Runtime::name).noneMatch(n -> n.equals(mimaRuntime.name()))) {
            runtimes.add(mimaRuntime);
        }
    }

    public synchronized void resetRuntimes() {
        runtimes.clear();
    }
}
