package org.cstamas.maven.mima.engines.smart;

import static java.util.Objects.requireNonNullElseGet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.core.engine.Engine;
import org.cstamas.maven.mima.core.engine.EngineSupport;
import org.cstamas.maven.mima.engines.maven.MavenEngine;
import org.cstamas.maven.mima.engines.standalone.StandaloneEngine;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class SmartEngine extends EngineSupport {

    @Inject
    private MavenEngine mavenEngine;

    private Engine engine;

    public SmartEngine() {
        super("smart");
    }

    @Override
    public MimaContext create(MimaContextOverrides overrides) {
        mayInitEngine();
        LoggerFactory.getLogger(getClass()).info("Using engine {}", engine.name());
        return engine.create(overrides);
    }

    private void mayInitEngine() {
        if (engine == null) {
            engine = requireNonNullElseGet(mavenEngine, StandaloneEngine::new);
        }
    }
}
