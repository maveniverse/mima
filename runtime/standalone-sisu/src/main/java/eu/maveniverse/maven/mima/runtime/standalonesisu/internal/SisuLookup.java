/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.Key;
import com.google.inject.name.Names;
import eu.maveniverse.maven.mima.context.Lookup;
import java.util.Iterator;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Provider;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.MutableBeanLocator;

public class SisuLookup implements Lookup {
    private final MutableBeanLocator locator;

    public SisuLookup(MutableBeanLocator locator) {
        this.locator = locator;
    }

    private <T> Optional<T> lookupInternal(Key<T> key) {
        final Iterable<? extends BeanEntry<Named, T>> entries = locator.locate(key);
        final Iterator<? extends BeanEntry<Named, T>> iterator = entries.iterator();
        final Provider<T> provider = iterator.hasNext() ? iterator.next().getProvider() : null;
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.of(provider.get());
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        return lookupInternal(Key.get(type, Named.class));
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        return lookupInternal(Key.get(type, Names.named(name)));
    }
}
