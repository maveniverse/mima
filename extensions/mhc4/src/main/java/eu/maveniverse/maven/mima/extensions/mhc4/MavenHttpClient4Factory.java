/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mhc4;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.extensions.mhc4.impl.MavenHttpClient4FactoryImpl;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Maven HttpClient 4.x factory. Creates Maven env configured {@link HttpClientBuilder} instance.
 * <p>
 * Configuration <em>not applied</em>, which should be handled by caller/user of this factory:
 * <ul>
 *     <li>most obviously, in Resolver the used remote repository base URI, in this case makes no sense</li>
 *     <li>Custom headers {@link ConfigurationProperties#HTTP_HEADERS}</li>
 *     <li>Preemptive authorization {@link ConfigurationProperties#HTTP_PREEMPTIVE_AUTH}</li>
 *     <li>Preemptive PUT authorization</li>
 * </ul>
 * <p>
 * Differences between {@link #createResolutionClient(RemoteRepository)} and {@link #createDeploymentClient(RemoteRepository)}
 * are exactly the same as in Maven/Resolver. See corresponding Javadoc.
 *
 * @see MavenHttpClient4FactoryImpl
 */
public class MavenHttpClient4Factory {
    protected final Context context;
    protected final MavenHttpClient4FactoryImpl factory;

    /**
     * Creates instance using passed in context.
     */
    public MavenHttpClient4Factory(Context context) {
        this.context = requireNonNull(context);
        this.factory = new MavenHttpClient4FactoryImpl(context.repositorySystem());
    }

    /**
     * Creates {@link HttpClientBuilder} preconfigured from Maven environment for resolving.
     */
    public HttpClientBuilder createResolutionClient(RemoteRepository repository) {
        return factory.createResolutionClient(context.repositorySystemSession(), repository);
    }

    /**
     * Creates {@link HttpClientBuilder} preconfigured from Maven environment for deployment.
     */
    public HttpClientBuilder createDeploymentClient(RemoteRepository repository) {
        return factory.createDeploymentClient(context.repositorySystemSession(), repository);
    }
}
