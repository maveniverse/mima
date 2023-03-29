package org.cstamas.maven.mima.impl.maven;

import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.cstamas.maven.mima.core.MimaResolver;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.core.engine.Engine;

public abstract class AbstractMimaMojo extends AbstractMojo {
    @Inject
    Engine engine;

    protected MimaResolver getResolver(MimaContextOverrides mimaContextOverrides) {
        return new MimaResolver(engine.create(mimaContextOverrides));
    }
}
