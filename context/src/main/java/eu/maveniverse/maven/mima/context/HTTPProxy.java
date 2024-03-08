/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

/**
 * HTTP Proxy configuration, that resolver uses.
 *
 * @since 2.4.0
 */
public final class HTTPProxy {
    private final String protocol;

    private final String host;

    private final int port;

    private final String nonProxyHosts;

    private final Map<String, Object> data;

    public HTTPProxy(String protocol, String host, int port, String nonProxyHosts, Map<String, Object> data) {
        this.protocol = requireNonNull(protocol);
        this.host = requireNonNull(host);
        this.port = port;
        this.nonProxyHosts = nonProxyHosts != null ? nonProxyHosts : "";
        this.data = data != null ? Collections.unmodifiableMap(data) : Collections.emptyMap();
    }

    /**
     * The protocol to use with HTTP Proxy, never {@code null}.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * The HTTP Proxy hostname, never {@code null}.
     */
    public String getHost() {
        return host;
    }

    /**
     * The HTTP Proxy port.
     */
    public int getPort() {
        return port;
    }

    /**
     * String of comma or pipe delimited list of non-proxy hosts, never {@code null}.
     *
     * @see <a href="https://maven.apache.org/settings.html#proxies">Maven Settings Reference - Proxies</a>
     */
    public String getNonProxyHosts() {
        return nonProxyHosts;
    }

    /**
     * Extra "data", like auth, never {@code null}.
     */
    public Map<String, Object> getData() {
        return data;
    }
}
