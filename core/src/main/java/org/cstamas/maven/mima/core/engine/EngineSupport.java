package org.cstamas.maven.mima.core.engine;

import static java.util.Objects.requireNonNull;

public abstract class EngineSupport implements Engine {
    private final String name;

    protected EngineSupport(String name) {
        this.name = requireNonNull(name);
    }

    @Override
    public String name() {
        return name;
    }
}
