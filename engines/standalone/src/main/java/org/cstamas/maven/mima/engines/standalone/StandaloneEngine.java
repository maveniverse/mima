package org.cstamas.maven.mima.engines.standalone;

import javax.inject.Named;
import javax.inject.Singleton;
import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.core.engine.EngineSupport;

@Singleton
@Named
public class StandaloneEngine extends EngineSupport {

    public StandaloneEngine() {
        super("standalone");
    }

    @Override
    public MimaContext create(MimaContextOverrides overrides) {
        return null;
    }
}
