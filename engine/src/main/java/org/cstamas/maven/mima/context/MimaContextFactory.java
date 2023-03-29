package org.cstamas.maven.mima.context;

public interface MimaContextFactory {
    default MimaContext create() {
        return create(MimaContextOverrides.Builder.create().build());
    }

    MimaContext create(MimaContextOverrides overrides);
}
