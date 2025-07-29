/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.extensions.mhc4;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.io.IOException;
import java.net.URI;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MavenHttpClient4FactoryTest {
    private final RemoteRepository httpCentral =
            new RemoteRepository.Builder("central-http", "default", "http://repo1.maven.org/maven2/").build();

    @Test
    void deploymentHttps() throws IOException {
        try (Context context = Runtimes.INSTANCE
                .getRuntime()
                .create(ContextOverrides.create()
                        .withUserSettings(true)
                        .build())) {
            MavenHttpClient4Factory factory = new MavenHttpClient4Factory(context);
            try (CloseableHttpClient client =
                    factory.createDeploymentClient(ContextOverrides.CENTRAL).build()) {
                try (CloseableHttpResponse response =
                        client.execute(new HttpHead(URI.create(ContextOverrides.CENTRAL.getUrl())
                                .resolve(".meta/prefixes.txt")
                                .toASCIIString()))) {
                    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                }
            }
        }
    }

    @Test
    void deploymentHttp() throws IOException {
        try (Context context = Runtimes.INSTANCE
                .getRuntime()
                .create(ContextOverrides.create()
                        .withUserSettings(true)
                        .build())) {
            MavenHttpClient4Factory factory = new MavenHttpClient4Factory(context);
            try (CloseableHttpClient client =
                    factory.createDeploymentClient(httpCentral).build()) {
                try (CloseableHttpResponse response = client.execute(new HttpHead(URI.create(httpCentral.getUrl())
                        .resolve(".meta/prefixes.txt")
                        .toASCIIString()))) {
                    Assertions.assertEquals(501, response.getStatusLine().getStatusCode());
                    Assertions.assertEquals(
                            "Varnish", response.getFirstHeader("Server").getValue());
                }
            }
        }
    }

    @Test
    void resolutionHttps() throws IOException {
        try (Context context = Runtimes.INSTANCE
                .getRuntime()
                .create(ContextOverrides.create()
                        .withUserSettings(true)
                        .build())) {
            MavenHttpClient4Factory factory = new MavenHttpClient4Factory(context);
            try (CloseableHttpClient client =
                    factory.createResolutionClient(ContextOverrides.CENTRAL).build()) {
                try (CloseableHttpResponse response =
                        client.execute(new HttpHead(URI.create(ContextOverrides.CENTRAL.getUrl())
                                .resolve(".meta/prefixes.txt")
                                .toASCIIString()))) {
                    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                }
            }
        }
    }

    @Test
    void resolutionHttp() {
        try (Context context = Runtimes.INSTANCE
                .getRuntime()
                .create(ContextOverrides.create()
                        .withUserSettings(true)
                        .build())) {
            MavenHttpClient4Factory factory = new MavenHttpClient4Factory(context);
            IllegalArgumentException e = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> factory.createResolutionClient(httpCentral));
            Assertions.assertTrue(e.getMessage().contains("blocked"));
        }
    }
}
