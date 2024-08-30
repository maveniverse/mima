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
import org.apache.maven.model.Model;

/**
 * Model Request.
 */
public final class ModelResponse {
    private final ModelRequest modelRequest;
    private final Map<ModelReaderMode, Model> models;

    public ModelResponse(ModelRequest modelRequest, Map<ModelReaderMode, Model> models) {
        this.modelRequest = requireNonNull(modelRequest);
        this.models = requireNonNull(models);
    }

    /**
     * Returns model in asked mode, may return {@code null}.
     */
    public Model getModel(ModelReaderMode mode) {
        requireNonNull(mode);
        return models.get(mode);
    }

    /**
     * Returns model in asked mode, may return {@code null}.
     */
    public Model getModel() {
        return getModel(modelRequest.getMode());
    }
}
