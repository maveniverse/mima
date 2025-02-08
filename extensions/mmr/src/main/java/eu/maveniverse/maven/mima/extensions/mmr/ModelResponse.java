/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Function;
import org.apache.maven.model.Model;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Model response carries models (raw and effective) but also offers some tools within the context of the read model.
 * <p>
 * One example would be something similar like done in Toolbox: getting raw model, modify it, interpolate it and finally
 * transform it to {@link ArtifactDescriptorResult} for friendlier after-processing in Resolver.
 */
public class ModelResponse {
    private final Model rawModel;
    private final Model effectiveModel;
    private final ArtifactRepository repository;
    private final Function<Model, ArtifactDescriptorResult> converter;
    private final List<String> lineage;
    private final Function<String, Model> lineageFunction;
    private final Function<Model, Model> interpolatorFunction;

    public ModelResponse(
            Model rawModel,
            Model effectiveModel,
            ArtifactRepository repository,
            Function<Model, ArtifactDescriptorResult> converter,
            List<String> lineage,
            Function<String, Model> lineageFunction,
            Function<Model, Model> interpolatorFunction) {
        this.rawModel = requireNonNull(rawModel);
        this.effectiveModel = requireNonNull(effectiveModel);
        this.repository = repository;
        this.converter = requireNonNull(converter);
        this.lineage = requireNonNull(lineage);
        this.lineageFunction = requireNonNull(lineageFunction);
        this.interpolatorFunction = requireNonNull(interpolatorFunction);
    }

    /**
     * Returns the built effective model.
     * <p>
     * If you intend to modify model but need to keep "original" as well, use method {@link Model#clone()} to clone the
     * model instance, otherwise you will end up with modified model in this result instance.
     */
    public Model getEffectiveModel() {
        return effectiveModel;
    }

    /**
     * Returns the "raw" (as is on disk) model.
     * <p>
     * If you intend to modify model but need to keep "original" as well, use method {@link Model#clone()} to clone the
     * model instance, otherwise you will end up with modified model in this result instance.
     */
    public Model getRawModel() {
        return rawModel;
    }

    /**
     * Gets the repository from which the descriptor was eventually resolved or {@code null} if unknown.
     */
    public ArtifactRepository getRepository() {
        return repository;
    }

    /**
     * Returns artifact descriptor result of given model.
     */
    public ArtifactDescriptorResult toArtifactDescriptorResult(Model model) {
        requireNonNull(model);
        return converter.apply(model);
    }

    /**
     * Returns the model "lineage" keys, first in list represents "current" model, last the Super POM, and parents in
     * middle.
     */
    public List<String> getLineage() {
        return lineage;
    }

    /**
     * Returns RAW model with given key (that is string concatenated as {@code groupId:artifactId:versionId}).
     *
     * @see #getLineage()
     */
    public Model getLineageModel(String modelId) {
        return lineageFunction.apply(modelId);
    }

    /**
     * Interpolates model. Make sense only on non-effective models, as effective models are already interpolated.
     * Uses value set provided by this "current" model. The model is first cloned, then interpolated, so the returned
     * instance is not the same instance as the passed in.
     */
    public Model interpolateModel(Model model) {
        return interpolatorFunction.apply(model);
    }
}
