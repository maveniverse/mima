/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Lookup;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import java.util.Optional;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;

/**
 * Certain classes that are not exposed via Maven supplier.
 */
public class CompatLookup implements Lookup {
    private final StringVisitorModelInterpolator stringVisitorModelInterpolator;

    public CompatLookup(PreBoot preBoot) {
        requireNonNull(preBoot);
        // StringVisitorModelInterpolator mvn3 vs mvn4 = field vs ctor injection, do DI should protect us from it
        this.stringVisitorModelInterpolator = new StringVisitorModelInterpolator();
        this.stringVisitorModelInterpolator
                .setPathTranslator(new DefaultPathTranslator())
                .setUrlNormalizer(new DefaultUrlNormalizer())
                .setVersionPropertiesProcessor(new DefaultModelVersionProcessor());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        if (type.isAssignableFrom(StringVisitorModelInterpolator.class)) {
            return (Optional<T>) Optional.of(stringVisitorModelInterpolator);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        if ("".equals(name) || "default".equals(name)) {
            return lookup(type);
        }
        return Optional.empty();
    }
}
