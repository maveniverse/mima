package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.Binder;
import com.google.inject.Module;

public class SisuModule implements Module {
    @Override
    public void configure(final Binder binder) {
        binder.bind(SisuBooter.class);
    }
}
