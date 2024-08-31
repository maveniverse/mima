/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.function.Function;
import org.apache.maven.model.Model;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Model response.
 */
public class ModelResponse {
    private final Map<ModelLevel, Model> models;
    private final Function<Model, ArtifactDescriptorResult> converter;

    public ModelResponse(Map<ModelLevel, Model> models, Function<Model, ArtifactDescriptorResult> converter) {
        this.models = requireNonNull(models);
        this.converter = requireNonNull(converter);
    }

    /**
     * Returns model in asked level, may return {@code null}.
     */
    public Model toModel(ModelLevel level) {
        requireNonNull(level);
        return models.get(level);
    }

    /**
     * Returns artifact descriptor result in asked level, may return {@code null}.
     */
    public ArtifactDescriptorResult toArtifactDescriptorResult(ModelLevel level) {
        return converter.apply(toModel(level));
    }
}
