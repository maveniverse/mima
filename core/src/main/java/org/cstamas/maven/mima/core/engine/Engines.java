package org.cstamas.maven.mima.core.engine;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.TreeSet;

public final class Engines {
    public static final Engines INSTANCE = new Engines();

    private final TreeSet<Engine> engines = new TreeSet<>(Comparator.comparing(Engine::priority));

    public synchronized Engine getEngine() {
        if (!engines.isEmpty()) {
            return engines.first();
        }

        ServiceLoader<Engine> loader = ServiceLoader.load(Engine.class);
        loader.stream().forEach(e -> engines.add(e.get()));
        if (engines.isEmpty()) {
            throw new IllegalStateException("No Engine implementation found on classpath");
        }
        return engines.first();
    }

    public synchronized void registerEngine(Engine engine) {
        requireNonNull(engine);
        engines.add(engine);
    }

    public synchronized void reset() {
        engines.clear();
    }
}
