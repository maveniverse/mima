package org.cstamas.maven.mima.engines.uber;

import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.core.engine.EngineSupport;

public class UberEngine extends EngineSupport {

    public UberEngine() {
        super("uber");
    }

    @Override
    public MimaContext create(MimaContextOverrides overrides) {
        throw new RuntimeException("not done");
    }
}
