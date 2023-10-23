package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

/**
 * Registry of known {@link Runtime} instances. It orders them by priority. This class is the "entry point" in MIMA to
 * obtain actual {@link Runtime} instance.
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
     * The comma or pipe delimited list of non-proxy hosts, never {@code null}.
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
