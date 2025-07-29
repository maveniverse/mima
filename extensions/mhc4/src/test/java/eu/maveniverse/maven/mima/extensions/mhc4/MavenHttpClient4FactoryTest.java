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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MavenHttpClient4FactoryTest {
    @Test
    void smokeHttps() throws IOException {
        try (Context context =
                Runtimes.INSTANCE.getRuntime().create(ContextOverrides.create().withUserSettings(true).build())) {
            MavenHttpClient4Factory factory = new MavenHttpClient4Factory(context);
            try (CloseableHttpClient client =
                    factory.createClient(ContextOverrides.CENTRAL).build()) {
                try (CloseableHttpResponse response =
                        client.execute(new HttpHead("https://repo.maven.apache.org/maven2/.meta/prefixes.txt"))) {
                    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                }
            }
        }
    }
}
