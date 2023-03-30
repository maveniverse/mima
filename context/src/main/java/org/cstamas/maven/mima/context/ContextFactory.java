package org.cstamas.maven.mima.context;

public interface ContextFactory {
    default Context create() {
        return create(ContextOverrides.Builder.create().build());
    }

    Context create(ContextOverrides overrides);
}
