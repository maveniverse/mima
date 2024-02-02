package eu.maveniverse.maven.mima.context;

import java.util.Map;
import java.util.Optional;

/**
 * A simple "lookup" that allows to lookup various components. Lookup shares lifecycle with {@link Context}.
 *
 * @since TBD
 */
public interface Lookup {
    /**
     * Performs lookup for component with passed in type, and returns it as optional, never {@code null}.
     */
    <T> Optional<T> lookup(Class<T> type);

    /**
     * Performs lookup for component with passed in type and name, and returns it as optional, never {@code null}.
     */
    <T> Optional<T> lookup(Class<T> type, String name);

    /**
     * Returns map of components for passed in type, never {@code null}.
     */
    <T> Map<String, T> lookupMap(Class<T> type);
}
