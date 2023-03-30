package org.cstamas.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Runtimes {
    public static final Runtimes INSTANCE = new Runtimes();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TreeSet<Runtime> runtimes = new TreeSet<>(Comparator.comparing(Runtime::priority));

    public synchronized Runtime getRuntime() {
        Runtime result = null;
        if (!runtimes.isEmpty()) {
            result = runtimes.first();
        }
        if (result == null) {
            ServiceLoader<Runtime> loader = ServiceLoader.load(Runtime.class);
            loader.stream().forEach(e -> registerRuntime(e.get()));
            if (runtimes.isEmpty()) {
                throw new IllegalStateException("No Runtime implementation found on classpath");
            }
            result = runtimes.first();
        }
        logger.debug("Runtimes.getRuntime: {}", result);
        return result;
    }

    public synchronized void registerRuntime(Runtime mimaRuntime) {
        requireNonNull(mimaRuntime);
        logger.debug("Runtimes.registerEngine: {}", mimaRuntime);
        runtimes.add(mimaRuntime);
    }

    public synchronized void resetRuntimes() {
        logger.debug("Runtimes.resetRuntimes");
        runtimes.clear();
    }
}
