package org.cstamas.maven.mima.engines.standalone;

import javax.inject.Named;
import javax.inject.Singleton;
import org.cstamas.maven.mima.context.MimaContext;
import org.cstamas.maven.mima.context.MimaContextOverrides;
import org.cstamas.maven.mima.engine.EngineSupport;

@Singleton
@Named
public class Standalone extends EngineSupport {
    @Override
    public MimaContext create(MimaContextOverrides overrides) {
        return null;
    }
}
