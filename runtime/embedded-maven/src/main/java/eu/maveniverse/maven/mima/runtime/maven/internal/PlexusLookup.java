package eu.maveniverse.maven.mima.runtime.maven.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Lookup;
import java.util.Optional;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public final class PlexusLookup implements Lookup {
    private final PlexusContainer plexusContainer;

    public PlexusLookup(PlexusContainer plexusContainer) {
        this.plexusContainer = requireNonNull(plexusContainer);
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        try {
            return Optional.of(plexusContainer.lookup(type));
        } catch (ComponentLookupException e) {
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        try {
            return Optional.of(plexusContainer.lookup(type, name));
        } catch (ComponentLookupException e) {
            return Optional.empty();
        }
    }
}
