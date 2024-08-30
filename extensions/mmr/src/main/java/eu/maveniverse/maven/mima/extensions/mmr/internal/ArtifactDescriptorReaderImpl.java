/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mmr.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.extensions.mmr.ModelLevel;
import eu.maveniverse.maven.mima.extensions.mmr.ModelResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelCache;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.apache.maven.repository.internal.RequestTraceHelper;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benjamin Bentmann
 */
public class ArtifactDescriptorReaderImpl {
    private final RepositorySystem repositorySystem;
    private final RemoteRepositoryManager remoteRepositoryManager;
    private final RepositoryEventDispatcher repositoryEventDispatcher;
    private final ModelBuilder modelBuilder;
    private final Function<RepositorySystemSession, ModelCache> modelCacheFunction;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ArtifactDescriptorReaderImpl(
            RepositorySystem repositorySystem,
            RemoteRepositoryManager remoteRepositoryManager,
            ModelBuilder modelBuilder,
            RepositoryEventDispatcher repositoryEventDispatcher,
            Function<RepositorySystemSession, ModelCache> modelCacheFunction) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.remoteRepositoryManager = requireNonNull(remoteRepositoryManager);
        this.modelBuilder = requireNonNull(modelBuilder);
        this.repositoryEventDispatcher = requireNonNull(repositoryEventDispatcher);
        this.modelCacheFunction = requireNonNull(modelCacheFunction);
    }

    public ModelResponse readArtifactDescriptor(RepositorySystemSession session, ArtifactDescriptorRequest request)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        ArtifactDescriptorResult artifactDescriptorResult = new ArtifactDescriptorResult(request);
        return new ModelResponse(
                loadPom(session, request, artifactDescriptorResult),
                m -> populateResult(session, artifactDescriptorResult, m));
    }

    private Map<ModelLevel, Model> loadPom(
            RepositorySystemSession session, ArtifactDescriptorRequest request, ArtifactDescriptorResult result)
            throws VersionResolutionException, ArtifactResolutionException, ArtifactDescriptorException {
        HashMap<ModelLevel, Model> resultMap = new HashMap<>();
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);
        Artifact a = request.getArtifact();

        Artifact pomArtifact = ArtifactDescriptorUtils.toPomArtifact(a);
        if (a.getFile() != null) {
            pomArtifact = pomArtifact.setFile(a.getFile());
        }

        ArtifactResult resolveResult = null;
        if (pomArtifact.getFile() == null) {
            try {
                VersionRequest versionRequest =
                        new VersionRequest(a, request.getRepositories(), request.getRequestContext());
                versionRequest.setTrace(trace);
                VersionResult versionResult = repositorySystem.resolveVersion(session, versionRequest);

                a = a.setVersion(versionResult.getVersion());

                versionRequest =
                        new VersionRequest(pomArtifact, request.getRepositories(), request.getRequestContext());
                versionRequest.setTrace(trace);
                versionResult = repositorySystem.resolveVersion(session, versionRequest);

                pomArtifact = pomArtifact.setVersion(versionResult.getVersion());
            } catch (VersionResolutionException e) {
                result.addException(e);
                throw e;
            }

            try {
                ArtifactRequest resolveRequest =
                        new ArtifactRequest(pomArtifact, request.getRepositories(), request.getRequestContext());
                resolveRequest.setTrace(trace);
                resolveResult = repositorySystem.resolveArtifact(session, resolveRequest);
                pomArtifact = resolveResult.getArtifact();
                result.setRepository(resolveResult.getRepository());
            } catch (ArtifactResolutionException e) {
                if (e.getCause() instanceof ArtifactNotFoundException) {
                    missingDescriptor(session, trace, a, (Exception) e.getCause());
                }
                result.addException(e);
                throw e;
            }
        }

        try {
            ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
            modelRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            modelRequest.setProcessPlugins(false);
            modelRequest.setTwoPhaseBuilding(false);
            // This merge is on purpose because otherwise user properties would override model
            // properties in dependencies the user does not know. See MNG-7563 for details.
            modelRequest.setSystemProperties(toProperties(session.getUserProperties(), session.getSystemProperties()));
            modelRequest.setUserProperties(new Properties());
            modelRequest.setModelCache(modelCacheFunction.apply(session));
            modelRequest.setModelResolver(new ModelResolverImpl(
                    repositorySystem,
                    session,
                    trace.newChild(modelRequest),
                    request.getRequestContext(),
                    remoteRepositoryManager,
                    request.getRepositories()));
            if (resolveResult != null && resolveResult.getRepository() instanceof WorkspaceRepository) {
                modelRequest.setPomFile(pomArtifact.getFile());
            } else {
                modelRequest.setModelSource(new FileModelSource(pomArtifact.getFile()));
            }

            ModelBuildingResult modelResult = modelBuilder.build(modelRequest);
            // ModelBuildingEx is thrown only on FATAL and ERROR severities, but we still can have WARNs
            // that may lead to unexpected build failure, log them
            if (!modelResult.getProblems().isEmpty()) {
                List<ModelProblem> problems = modelResult.getProblems();
                if (logger.isDebugEnabled()) {
                    String problem = (problems.size() == 1) ? "problem" : "problems";
                    String problemPredicate = problem + ((problems.size() == 1) ? " was" : " were");
                    String message = String.format(
                            "%s %s encountered while building the effective model for %s during %s\n",
                            problems.size(),
                            problemPredicate,
                            request.getArtifact(),
                            RequestTraceHelper.interpretTrace(true, request.getTrace()));
                    message += StringUtils.capitalizeFirstLetter(problem);
                    for (ModelProblem modelProblem : problems) {
                        message += String.format(
                                "\n* %s @ %s",
                                modelProblem.getMessage(), ModelProblemUtils.formatLocation(modelProblem, null));
                    }
                    logger.warn(message);
                } else {
                    logger.warn(
                            "{} {} encountered while building the effective model for {} during {} (use -X to see details)",
                            problems.size(),
                            (problems.size() == 1) ? "problem was" : "problems were",
                            request.getArtifact(),
                            RequestTraceHelper.interpretTrace(false, request.getTrace()));
                }
            }

            // raw
            resultMap.put(ModelLevel.RAW, modelResult.getRawModel());

            // raw+interpolated
            Model rawModel = modelResult.getRawModel();
            rawModel.setGroupId(modelResult.getEffectiveModel().getGroupId());
            rawModel.setArtifactId(modelResult.getEffectiveModel().getArtifactId());
            rawModel.setVersion(modelResult.getEffectiveModel().getVersion());
            rawModel.setProperties(modelResult.getEffectiveModel().getProperties());
            Model current = rawModel;
            while (current.getParent() != null) {
                String parentId = current.getParent().getGroupId() + ":"
                        + current.getParent().getArtifactId() + ":"
                        + current.getParent().getVersion();
                Model parent = modelResult.getRawModel(parentId);
                if (parent.getDependencyManagement() != null) {
                    if (rawModel.getDependencyManagement() == null) {
                        rawModel.setDependencyManagement(new DependencyManagement());
                    }
                    parent.getDependencyManagement().getDependencies().forEach(d -> rawModel.getDependencyManagement()
                            .addDependency(d));
                }
                current = parent;
            }
            resultMap.put(
                    ModelLevel.RAW_INTERPOLATED,
                    new StringVisitorModelInterpolator()
                            .setPathTranslator(new DefaultPathTranslator())
                            .setUrlNormalizer(new DefaultUrlNormalizer())
                            .setVersionPropertiesProcessor(new DefaultModelVersionProcessor())
                            .interpolateModel(modelResult.getRawModel(), new File(""), modelRequest, req -> {}));

            // effective
            resultMap.put(ModelLevel.EFFECTIVE, modelResult.getEffectiveModel());

            return resultMap;
        } catch (ModelBuildingException e) {
            for (ModelProblem problem : e.getProblems()) {
                if (problem.getException() instanceof UnresolvableModelException) {
                    result.addException(problem.getException());
                    throw new ArtifactDescriptorException(result);
                }
            }
            invalidDescriptor(session, trace, a, e);
            result.addException(e);
            throw new ArtifactDescriptorException(result);
        }
    }

    private Properties toProperties(Map<String, String> dominant, Map<String, String> recessive) {
        Properties props = new Properties();
        if (recessive != null) {
            props.putAll(recessive);
        }
        if (dominant != null) {
            props.putAll(dominant);
        }
        return props;
    }

    private void missingDescriptor(
            RepositorySystemSession session, RequestTrace trace, Artifact artifact, Exception exception) {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, EventType.ARTIFACT_DESCRIPTOR_MISSING);
        event.setTrace(trace);
        event.setArtifact(artifact);
        event.setException(exception);

        repositoryEventDispatcher.dispatch(event.build());
    }

    private void invalidDescriptor(
            RepositorySystemSession session, RequestTrace trace, Artifact artifact, Exception exception) {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, EventType.ARTIFACT_DESCRIPTOR_INVALID);
        event.setTrace(trace);
        event.setArtifact(artifact);
        event.setException(exception);

        repositoryEventDispatcher.dispatch(event.build());
    }

    // ArtifactDescriptorResult

    private static ArtifactDescriptorResult populateResult(
            RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
        ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

        for (Repository r : model.getRepositories()) {
            result.addRepository(ArtifactDescriptorUtils.toRemoteRepository(r));
        }

        for (org.apache.maven.model.Dependency dependency : model.getDependencies()) {
            result.addDependency(convert(dependency, stereotypes));
        }

        DependencyManagement mgmt = model.getDependencyManagement();
        if (mgmt != null) {
            for (org.apache.maven.model.Dependency dependency : mgmt.getDependencies()) {
                result.addManagedDependency(convert(dependency, stereotypes));
            }
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("model", model);
        Prerequisites prerequisites = model.getPrerequisites();
        if (prerequisites != null) {
            properties.put("prerequisites.maven", prerequisites.getMaven());
        }

        List<License> licenses = model.getLicenses();
        properties.put("license.count", licenses.size());
        for (int i = 0; i < licenses.size(); i++) {
            License license = licenses.get(i);
            properties.put("license." + i + ".name", license.getName());
            properties.put("license." + i + ".url", license.getUrl());
            properties.put("license." + i + ".comments", license.getComments());
            properties.put("license." + i + ".distribution", license.getDistribution());
        }
        result.setProperties(properties);
        setArtifactProperties(result, model);
        return result;
    }

    private static Dependency convert(org.apache.maven.model.Dependency dependency, ArtifactTypeRegistry stereotypes) {
        ArtifactType stereotype = stereotypes.get(dependency.getType());
        if (stereotype == null) {
            stereotype = new DefaultArtifactType(dependency.getType());
        }

        boolean system = dependency.getSystemPath() != null
                && !dependency.getSystemPath().isEmpty();

        Map<String, String> props = null;
        if (system) {
            props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, dependency.getSystemPath());
        }

        Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                null,
                dependency.getVersion(),
                props,
                stereotype);

        List<Exclusion> exclusions = new ArrayList<>(dependency.getExclusions().size());
        for (org.apache.maven.model.Exclusion exclusion : dependency.getExclusions()) {
            exclusions.add(convert(exclusion));
        }

        return new Dependency(
                artifact,
                dependency.getScope(),
                dependency.getOptional() != null ? dependency.isOptional() : null,
                exclusions);
    }

    private static Exclusion convert(org.apache.maven.model.Exclusion exclusion) {
        return new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*");
    }

    private static void setArtifactProperties(ArtifactDescriptorResult result, Model model) {
        String downloadUrl = null;
        DistributionManagement distMgmt = model.getDistributionManagement();
        if (distMgmt != null) {
            downloadUrl = distMgmt.getDownloadUrl();
        }
        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            Artifact artifact = result.getArtifact();
            Map<String, String> props = new HashMap<>(artifact.getProperties());
            props.put(ArtifactProperties.DOWNLOAD_URL, downloadUrl);
            result.setArtifact(artifact.setProperties(props));
        }
    }
}
