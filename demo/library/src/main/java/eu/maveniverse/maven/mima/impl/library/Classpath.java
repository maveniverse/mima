/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.impl.library;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReader;
import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReaderMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an imaginary library class that wants to:
 * <ul>
 *     <li>use Resolver API and components</li>
 *     <li>needs to work standalone, but also embedded in Maven (ie as a plugin dependency)</li>
 * </ul>
 */
public class Classpath {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String model(ContextOverrides overrides, String artifactStr)
            throws ArtifactDescriptorException, IOException {
        requireNonNull(artifactStr);
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        logger.debug("Runtimes.getRuntime: {}", runtime);

        try (Context context = runtime.create(overrides)) {
            MavenModelReader mmr = new MavenModelReader(context);
            ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(
                    new DefaultArtifact(artifactStr), context.remoteRepositories(), "classpath-demo");
            Model model = mmr.readModel(request, MavenModelReaderMode.EFFECTIVE);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                String encoding = model.getModelEncoding();
                if (encoding == null || encoding.length() <= 0) {
                    encoding = "UTF-8";
                }

                try (Writer out = new OutputStreamWriter(outputStream, encoding)) {
                    new MavenXpp3Writer().write(out, model);
                }
                return outputStream.toString(encoding);
            }
        }
    }

    public String classpath(ContextOverrides overrides, String artifactStr) throws DependencyResolutionException {
        requireNonNull(artifactStr);
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        logger.debug("Runtimes.getRuntime: {}", runtime);

        // ad-hoc: create context w/ or w/o overrides
        // other way is to make this class manage context or manage context outside it
        // depends what you need: one shot or reuse of MIMA instance
        try (Context context = runtime.create(overrides)) {
            DefaultArtifact artifact = new DefaultArtifact(artifactStr);
            logger.info("doClasspath: {}", context.remoteRepositories());

            RemoteRepositoryManager remoteRepositoryManager = context.lookup()
                    .lookup(RemoteRepositoryManager.class)
                    .orElseThrow(() -> new IllegalStateException("component not found"));
            for (RemoteRepository repository : context.remoteRepositories()) {
                RepositoryPolicy policy = remoteRepositoryManager.getPolicy(
                        context.repositorySystemSession(), repository, !artifact.isSnapshot(), artifact.isSnapshot());
                logger.info("Repository {} effective policy: {}", repository.getId(), policy);
            }

            Dependency dependency = new Dependency(artifact, "runtime");
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.setRepositories(context.remoteRepositories());

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setCollectRequest(collectRequest);

            DependencyNode rootNode = context.repositorySystem()
                    .resolveDependencies(context.repositorySystemSession(), dependencyRequest)
                    .getRoot();

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            rootNode.accept(nlg);
            return nlg.getClassPath();
        }
    }

    public static void main(String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("g:a:v");
        }
        Classpath classpath = new Classpath();
        try {
            ContextOverrides overrides =
                    ContextOverrides.create().withUserSettings(true).build();

            String cp = classpath.classpath(overrides, args[0]);
            System.out.println("Classpath of " + args[0] + " is:");
            System.out.println(cp);
        } catch (DependencyResolutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
