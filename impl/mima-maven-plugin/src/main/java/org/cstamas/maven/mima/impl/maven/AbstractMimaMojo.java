package org.cstamas.maven.mima.impl.maven;

import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.cstamas.maven.mima.core.MimaResolver;
import org.cstamas.maven.mima.core.context.MimaContext;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.core.engine.Engine;
import org.cstamas.maven.mima.engines.standalone.StandaloneEngine;
import org.eclipse.sisu.Nullable;

public abstract class AbstractMimaMojo extends AbstractMojo {
    @Inject
    @Nullable
    Engine engine;

    protected MimaResolver getResolver(MimaContextOverrides mimaContextOverrides) {
        MimaContext mimaContext;
        if (engine == null) {
            engine = new StandaloneEngine();
        }
        return new MimaResolver(engine.create(mimaContextOverrides));
    }
}
