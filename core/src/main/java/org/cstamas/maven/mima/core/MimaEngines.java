package org.cstamas.maven.mima.core;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.TreeSet;

public final class MimaEngines {
    public static final MimaEngines INSTANCE = new MimaEngines();

    private final TreeSet<MimaEngine> mimaEngines = new TreeSet<>(Comparator.comparing(MimaEngine::priority));

    public synchronized MimaEngine getEngine() {
        if (!mimaEngines.isEmpty()) {
            return mimaEngines.first();
        }

        ServiceLoader<MimaEngine> loader = ServiceLoader.load(MimaEngine.class);
        loader.stream().forEach(e -> mimaEngines.add(e.get()));
        if (mimaEngines.isEmpty()) {
            throw new IllegalStateException("No Engine implementation found on classpath");
        }
        return mimaEngines.first();
    }

    public synchronized void registerEngine(MimaEngine mimaEngine) {
        requireNonNull(mimaEngine);
        mimaEngines.add(mimaEngine);
    }

    public synchronized void reset() {
        mimaEngines.clear();
    }
}
