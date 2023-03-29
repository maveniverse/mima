package org.cstamas.maven.mima.impl.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.cstamas.maven.mima.core.MimaResolver;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.cstamas.maven.mima.engines.smart.SmartEngine;

public abstract class AbstractMimaMojo extends AbstractMojo {
    @Component
    SmartEngine engine;

    protected MimaResolver getResolver(MimaContextOverrides mimaContextOverrides) {
        return new MimaResolver(engine.create(mimaContextOverrides));
    }
}
