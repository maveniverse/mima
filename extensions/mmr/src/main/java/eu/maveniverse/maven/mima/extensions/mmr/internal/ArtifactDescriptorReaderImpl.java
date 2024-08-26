/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package eu.maveniverse.maven.mima.extensions.mmr.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
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
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.apache.maven.repository.internal.RequestTraceHelper;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicyRequest;
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
    private final ArtifactDescriptorReaderDelegate artifactDescriptorReaderDelegate =
            new ArtifactDescriptorReaderDelegate();

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

    public ArtifactDescriptorResult readEffectiveArtifactDescriptor(
            RepositorySystemSession session, ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        return readArtifactDescriptor(session, request, true);
    }

    public ArtifactDescriptorResult readRawArtifactDescriptor(
            RepositorySystemSession session, ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        return readArtifactDescriptor(session, request, false);
    }

    private ArtifactDescriptorResult readArtifactDescriptor(
            RepositorySystemSession session, ArtifactDescriptorRequest request, boolean effective)
            throws ArtifactDescriptorException {
        ArtifactDescriptorResult result = new ArtifactDescriptorResult(request);

        Model model = loadPom(session, request, result, effective);
        if (model != null) {
            Map<String, Object> config = session.getConfigProperties();
            ArtifactDescriptorReaderDelegate delegate =
                    (ArtifactDescriptorReaderDelegate) config.get(ArtifactDescriptorReaderDelegate.class.getName());

            if (delegate == null) {
                delegate = artifactDescriptorReaderDelegate;
            }

            delegate.populateResult(session, result, model);
        }

        return result;
    }

    private Model loadPom(
            RepositorySystemSession session,
            ArtifactDescriptorRequest request,
            ArtifactDescriptorResult result,
            boolean effective)
            throws ArtifactDescriptorException {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        Set<String> visited = new LinkedHashSet<>();
        for (Artifact a = request.getArtifact(); ; ) {
            Artifact pomArtifact = ArtifactDescriptorUtils.toPomArtifact(a);
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
                throw new ArtifactDescriptorException(result);
            }

            if (!visited.add(a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getBaseVersion())) {
                RepositoryException exception =
                        new RepositoryException("Artifact relocations form a cycle: " + visited);
                invalidDescriptor(session, trace, a, exception);
                if ((getPolicy(session, a, request) & ArtifactDescriptorPolicy.IGNORE_INVALID) != 0) {
                    return null;
                }
                result.addException(exception);
                throw new ArtifactDescriptorException(result);
            }

            ArtifactResult resolveResult;
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
                    if ((getPolicy(session, a, request) & ArtifactDescriptorPolicy.IGNORE_MISSING) != 0) {
                        return null;
                    }
                }
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            Model model;

            // TODO hack: don't rebuild model if it was already loaded during reactor resolution
            final WorkspaceReader workspace = session.getWorkspaceReader();
            if (workspace instanceof MavenWorkspaceReader) {
                model = ((MavenWorkspaceReader) workspace).findModel(pomArtifact);
                if (model != null) {
                    return model;
                }
            }

            try {
                ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
                modelRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                modelRequest.setProcessPlugins(false);
                modelRequest.setTwoPhaseBuilding(false);
                // This merge is on purpose because otherwise user properties would override model
                // properties in dependencies the user does not know. See MNG-7563 for details.
                modelRequest.setSystemProperties(
                        toProperties(session.getUserProperties(), session.getSystemProperties()));
                modelRequest.setUserProperties(new Properties());
                modelRequest.setModelCache(modelCacheFunction.apply(session));
                modelRequest.setModelResolver(new ModelResolverImpl(
                        repositorySystem,
                        session,
                        trace.newChild(modelRequest),
                        request.getRequestContext(),
                        remoteRepositoryManager,
                        request.getRepositories()));
                if (resolveResult.getRepository() instanceof WorkspaceRepository) {
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
                if (effective) {
                    model = modelResult.getEffectiveModel();
                } else {
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
                            parent.getDependencyManagement()
                                    .getDependencies()
                                    .forEach(d ->
                                            rawModel.getDependencyManagement().addDependency(d));
                        }
                        current = parent;
                    }

                    return new StringVisitorModelInterpolator()
                            .setPathTranslator(new DefaultPathTranslator())
                            .setUrlNormalizer(new DefaultUrlNormalizer())
                            .setVersionPropertiesProcessor(new DefaultModelVersionProcessor())
                            .interpolateModel(modelResult.getRawModel(), new File(""), modelRequest, req -> {});
                }
            } catch (ModelBuildingException e) {
                for (ModelProblem problem : e.getProblems()) {
                    if (problem.getException() instanceof UnresolvableModelException) {
                        result.addException(problem.getException());
                        throw new ArtifactDescriptorException(result);
                    }
                }
                invalidDescriptor(session, trace, a, e);
                if ((getPolicy(session, a, request) & ArtifactDescriptorPolicy.IGNORE_INVALID) != 0) {
                    return null;
                }
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            Relocation relocation = getRelocation(model);

            if (relocation != null) {
                result.addRelocation(a);
                a = new RelocatedArtifact(
                        a,
                        relocation.getGroupId(),
                        relocation.getArtifactId(),
                        relocation.getVersion(),
                        relocation.getMessage());
                result.setArtifact(a);
            } else {
                return model;
            }
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

    private Relocation getRelocation(Model model) {
        Relocation relocation = null;
        DistributionManagement distMgmt = model.getDistributionManagement();
        if (distMgmt != null) {
            relocation = distMgmt.getRelocation();
        }
        return relocation;
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

    private int getPolicy(RepositorySystemSession session, Artifact a, ArtifactDescriptorRequest request) {
        ArtifactDescriptorPolicy policy = session.getArtifactDescriptorPolicy();
        if (policy == null) {
            return ArtifactDescriptorPolicy.STRICT;
        }
        return policy.getPolicy(session, new ArtifactDescriptorPolicyRequest(a, request.getRequestContext()));
    }
}
