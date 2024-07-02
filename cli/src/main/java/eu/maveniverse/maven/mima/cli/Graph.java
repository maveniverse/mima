/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import picocli.CommandLine;

/**
 * Collects given GAV and output its dependency graph.
 */
@CommandLine.Command(name = "graph", description = "Displays dependency graph")
public final class Graph extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to graph")
    private String gav;

    @CommandLine.Option(
            names = {"--excludeScopes"},
            defaultValue = JavaScopes.TEST,
            split = ",",
            description = "Scopes to exclude (default is 'test')")
    private String[] excludeScopes;

    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            split = ",",
            description = "Comma separated list of BOMs to apply")
    private String[] boms;

    @Override
    protected Integer doCall(Context context) throws Exception {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        RepositorySystem repositorySystem = context.repositorySystem();

        session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
        session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

        session.setDependencySelector(new AndDependencySelector(
                new ScopeDependencySelector(excludeScopes),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()));
        session.setDependencyTraverser(new FatArtifactTraverser());

        session.setDependencyGraphTransformer(new ChainedDependencyGraphTransformer(
                new ConflictResolver(
                        new NearestVersionSelector(), new JavaScopeSelector(),
                        new SimpleOptionalitySelector(), new JavaScopeDeriver()),
                new JavaDependencyContextRefiner()));

        java.util.List<Dependency> managedDependencies = importBoms(context, boms);
        Artifact artifact = parseGav(gav, managedDependencies);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.setRepositories(context.remoteRepositories());
        collectRequest.setManagedDependencies(managedDependencies);

        verbose("Collecting {}", collectRequest);
        repositorySystem
                .collectDependencies(session, collectRequest)
                .getRoot()
                .accept(new DependencyGraphDumper(this::info));
        return 0;
    }
}
