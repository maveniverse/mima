package org.cstamas.maven.mima.engines.smart.internal;

import javax.inject.Named;
import org.eclipse.sisu.EagerSingleton;
import org.slf4j.LoggerFactory;

@Named
@EagerSingleton
public class Bootstrap {
    public Bootstrap() {
        LoggerFactory.getLogger(getClass()).info("Here!!!");
    }
}
