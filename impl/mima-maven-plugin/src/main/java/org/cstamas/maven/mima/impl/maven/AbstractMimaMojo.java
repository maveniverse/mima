package org.cstamas.maven.mima.impl.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.cstamas.maven.mima.core.MimaResolver;
import org.cstamas.maven.mima.core.engine.Engine;

public abstract class AbstractMimaMojo extends AbstractMojo {
    @Component
    Engine engine;

    protected MimaResolver getResolver() {
        return new MimaResolver(engine.create());
    }
}
