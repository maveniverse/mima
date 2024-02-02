package eu.maveniverse.maven.mima.context.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Lookup;
import java.util.*;

/**
 * A {@link Lookup} implementation that is able to iterate through several lookups, applying "first deliver wins"
 * strategy.
 *
 * @since TBD
 */
public final class IteratingLookup implements Lookup {
    private final Collection<Lookup> lookups;

    public IteratingLookup(Lookup... lookups) {
        this(Arrays.asList(lookups));
    }

    public IteratingLookup(Collection<Lookup> lookups) {
        this.lookups = requireNonNull(lookups);
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        for (Lookup lookup : lookups) {
            Optional<T> result = lookup.lookup(type);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        for (Lookup lookup : lookups) {
            Optional<T> result = lookup.lookup(type, name);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
