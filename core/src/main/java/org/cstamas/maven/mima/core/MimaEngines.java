package org.cstamas.maven.mima.core;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MimaEngines {
    public static final MimaEngines INSTANCE = new MimaEngines();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TreeSet<MimaEngine> mimaEngines = new TreeSet<>(Comparator.comparing(MimaEngine::priority));

    public synchronized MimaEngine getEngine() {
        MimaEngine result = null;
        if (!mimaEngines.isEmpty()) {
            result = mimaEngines.first();
        }
        if (result == null) {
            ServiceLoader<MimaEngine> loader = ServiceLoader.load(MimaEngine.class);
            loader.stream().forEach(e -> registerEngine(e.get()));
            if (mimaEngines.isEmpty()) {
                throw new IllegalStateException("No Engine implementation found on classpath");
            }
            result = mimaEngines.first();
        }
        logger.debug("MimeEngines.getEngine: {}", result);
        return result;
    }

    public synchronized void registerEngine(MimaEngine mimaEngine) {
        requireNonNull(mimaEngine);
        logger.debug("MimeEngines.registerEngine: {}", mimaEngine);
        mimaEngines.add(mimaEngine);
    }

    public synchronized void reset() {
        logger.debug("registerEngine.reset");
        mimaEngines.clear();
    }
}
