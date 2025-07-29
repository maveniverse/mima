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
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLInitializationException;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Maven HttpClient 4.x factory.
 */
public class MavenHttpClient4Factory {
    protected static final String BIND_ADDRESS = "aether.connector.bind.address";

    protected static final String PREEMPTIVE_PUT_AUTH = "aether.connector.http.preemptivePutAuth";

    protected static final String USE_SYSTEM_PROPERTIES = "aether.connector.http.useSystemProperties";

    protected static final String HTTP_RETRY_HANDLER_NAME = "aether.connector.http.retryHandler.name";

    protected static final String HTTP_RETRY_HANDLER_NAME_STANDARD = "standard";

    protected static final String HTTP_RETRY_HANDLER_NAME_DEFAULT = "default";

    protected static final String HTTP_RETRY_HANDLER_REQUEST_SENT_ENABLED =
            "aether.connector.http.retryHandler.requestSentEnabled";

    protected final Context context;

    /**
     * Creates instance using passed in context.
     */
    public MavenHttpClient4Factory(Context context) {
        this.context = requireNonNull(context);
    }

    /**
     * Creates {@link HttpClientBuilder} preconfigured from Maven environment.
     */
    public HttpClientBuilder createClient(RemoteRepository repository) {
        requireNonNull(repository, "repository");
        repository = context.repositorySystem().newDeploymentRepository(context.repositorySystemSession(), repository);
        RepositorySystemSession session = context.repositorySystemSession();

        URI baseUri;
        HttpHost server;
        try {
            baseUri = new URI(repository.getUrl()).parseServerAuthority();
            if (baseUri.isOpaque()) {
                throw new URISyntaxException(repository.getUrl(), "URL must not be opaque");
            }
            server = URIUtils.extractHost(baseUri);
            if (server == null) {
                throw new URISyntaxException(repository.getUrl(), "URL lacks host name");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Illegal RemoteRepository " + repository, e);
        }
        HttpHost proxy = toHost(repository.getProxy());

        AuthenticationContext repoAuthContext = AuthenticationContext.forRepository(session, repository);
        AuthenticationContext proxyAuthContext = AuthenticationContext.forProxy(session, repository);

        String httpsSecurityMode = ConfigUtils.getString(
                session,
                ConfigurationProperties.HTTPS_SECURITY_MODE_DEFAULT,
                ConfigurationProperties.HTTPS_SECURITY_MODE + "." + repository.getId(),
                ConfigurationProperties.HTTPS_SECURITY_MODE);
        final int connectionMaxTtlSeconds = ConfigUtils.getInteger(
                session,
                ConfigurationProperties.DEFAULT_HTTP_CONNECTION_MAX_TTL,
                ConfigurationProperties.HTTP_CONNECTION_MAX_TTL + "." + repository.getId(),
                ConfigurationProperties.HTTP_CONNECTION_MAX_TTL);
        final int maxConnectionsPerRoute = ConfigUtils.getInteger(
                session,
                ConfigurationProperties.DEFAULT_HTTP_MAX_CONNECTIONS_PER_ROUTE,
                ConfigurationProperties.HTTP_MAX_CONNECTIONS_PER_ROUTE + "." + repository.getId(),
                ConfigurationProperties.HTTP_MAX_CONNECTIONS_PER_ROUTE);

        HttpClientConnectionManager connectionManager = newConnectionManager(new ConnMgrConfig(
                session, repoAuthContext, httpsSecurityMode, connectionMaxTtlSeconds, maxConnectionsPerRoute));

        // TODO
        boolean preemptiveAuth = ConfigUtils.getBoolean(
                session,
                ConfigurationProperties.DEFAULT_HTTP_PREEMPTIVE_AUTH,
                ConfigurationProperties.HTTP_PREEMPTIVE_AUTH + "." + repository.getId(),
                ConfigurationProperties.HTTP_PREEMPTIVE_AUTH);
        // TODO
        boolean preemptivePutAuth = // defaults to true: Wagon does same
                ConfigUtils.getBoolean(
                        session, true, PREEMPTIVE_PUT_AUTH + "." + repository.getId(), PREEMPTIVE_PUT_AUTH);
        String credentialEncoding = ConfigUtils.getString(
                session,
                ConfigurationProperties.DEFAULT_HTTP_CREDENTIAL_ENCODING,
                ConfigurationProperties.HTTP_CREDENTIAL_ENCODING + "." + repository.getId(),
                ConfigurationProperties.HTTP_CREDENTIAL_ENCODING);
        int connectTimeout = ConfigUtils.getInteger(
                session,
                ConfigurationProperties.DEFAULT_CONNECT_TIMEOUT,
                ConfigurationProperties.CONNECT_TIMEOUT + "." + repository.getId(),
                ConfigurationProperties.CONNECT_TIMEOUT);
        int requestTimeout = ConfigUtils.getInteger(
                session,
                ConfigurationProperties.DEFAULT_REQUEST_TIMEOUT,
                ConfigurationProperties.REQUEST_TIMEOUT + "." + repository.getId(),
                ConfigurationProperties.REQUEST_TIMEOUT);
        int retryCount = ConfigUtils.getInteger(
                session,
                ConfigurationProperties.DEFAULT_HTTP_RETRY_HANDLER_COUNT,
                ConfigurationProperties.HTTP_RETRY_HANDLER_COUNT + "." + repository.getId(),
                ConfigurationProperties.HTTP_RETRY_HANDLER_COUNT);
        long retryInterval = ConfigUtils.getLong(
                session,
                ConfigurationProperties.DEFAULT_HTTP_RETRY_HANDLER_INTERVAL,
                ConfigurationProperties.HTTP_RETRY_HANDLER_INTERVAL + "." + repository.getId(),
                ConfigurationProperties.HTTP_RETRY_HANDLER_INTERVAL);
        long retryIntervalMax = ConfigUtils.getLong(
                session,
                ConfigurationProperties.DEFAULT_HTTP_RETRY_HANDLER_INTERVAL_MAX,
                ConfigurationProperties.HTTP_RETRY_HANDLER_INTERVAL_MAX + "." + repository.getId(),
                ConfigurationProperties.HTTP_RETRY_HANDLER_INTERVAL_MAX);
        String serviceUnavailableCodesString = ConfigUtils.getString(
                session,
                ConfigurationProperties.DEFAULT_HTTP_RETRY_HANDLER_SERVICE_UNAVAILABLE,
                ConfigurationProperties.HTTP_RETRY_HANDLER_SERVICE_UNAVAILABLE + "." + repository.getId(),
                ConfigurationProperties.HTTP_RETRY_HANDLER_SERVICE_UNAVAILABLE);
        String retryHandlerName = ConfigUtils.getString(
                session,
                HTTP_RETRY_HANDLER_NAME_STANDARD,
                HTTP_RETRY_HANDLER_NAME + "." + repository.getId(),
                HTTP_RETRY_HANDLER_NAME);
        boolean retryHandlerRequestSentEnabled = ConfigUtils.getBoolean(
                session,
                false,
                HTTP_RETRY_HANDLER_REQUEST_SENT_ENABLED + "." + repository.getId(),
                HTTP_RETRY_HANDLER_REQUEST_SENT_ENABLED);
        String userAgent = ConfigUtils.getString(
                session, ConfigurationProperties.DEFAULT_USER_AGENT, ConfigurationProperties.USER_AGENT);
        int maxRedirects = ConfigUtils.getInteger(
                session,
                ConfigurationProperties.DEFAULT_HTTP_MAX_REDIRECTS,
                ConfigurationProperties.HTTP_MAX_REDIRECTS + "." + repository.getId(),
                ConfigurationProperties.HTTP_MAX_REDIRECTS);
        boolean followRedirects = ConfigUtils.getBoolean(
                session,
                ConfigurationProperties.DEFAULT_FOLLOW_REDIRECTS,
                ConfigurationProperties.HTTP_FOLLOW_REDIRECTS + "." + repository.getId(),
                ConfigurationProperties.HTTP_FOLLOW_REDIRECTS);
        final String expectContinue = ConfigUtils.getString(
                session,
                null,
                ConfigurationProperties.HTTP_EXPECT_CONTINUE + "." + repository.getId(),
                ConfigurationProperties.HTTP_EXPECT_CONTINUE);

        Charset credentialsCharset = Charset.forName(credentialEncoding);
        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory(credentialsCharset))
                .register(AuthSchemes.DIGEST, new DigestSchemeFactory(credentialsCharset))
                .build();
        SocketConfig socketConfig =
                SocketConfig.custom().setSoTimeout(requestTimeout).build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setMaxRedirects(maxRedirects)
                .setRedirectsEnabled(followRedirects)
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectTimeout)
                .setLocalAddress(getHttpLocalAddress(session, repository))
                .setCookieSpec(CookieSpecs.STANDARD)
                .setSocketTimeout(requestTimeout)
                .setExpectContinueEnabled(Boolean.parseBoolean(expectContinue))
                .build();

        HttpRequestRetryHandler retryHandler;
        if (HTTP_RETRY_HANDLER_NAME_STANDARD.equals(retryHandlerName)) {
            retryHandler = new StandardHttpRequestRetryHandler(retryCount, retryHandlerRequestSentEnabled);
        } else if (HTTP_RETRY_HANDLER_NAME_DEFAULT.equals(retryHandlerName)) {
            retryHandler = new DefaultHttpRequestRetryHandler(retryCount, retryHandlerRequestSentEnabled);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported parameter " + HTTP_RETRY_HANDLER_NAME + " value: " + retryHandlerName);
        }
        Set<Integer> serviceUnavailableCodes = new HashSet<>();
        try {
            for (String code : ConfigUtils.parseCommaSeparatedUniqueNames(serviceUnavailableCodesString)) {
                serviceUnavailableCodes.add(Integer.parseInt(code));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Illegal HTTP codes for " + ConfigurationProperties.HTTP_RETRY_HANDLER_SERVICE_UNAVAILABLE
                            + " (list of integers): " + serviceUnavailableCodesString);
        }
        ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy = new ResolverServiceUnavailableRetryStrategy(
                retryCount, retryInterval, retryIntervalMax, serviceUnavailableCodes);

        HttpClientBuilder builder = HttpClientBuilder.create()
                .setUserAgent(userAgent)
                .setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
                .setDefaultSocketConfig(socketConfig)
                .setDefaultRequestConfig(requestConfig)
                .setServiceUnavailableRetryStrategy(serviceUnavailableRetryStrategy)
                .setRetryHandler(retryHandler)
                .setDefaultAuthSchemeRegistry(authSchemeRegistry)
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .setDefaultCredentialsProvider(toCredentialsProvider(server, repoAuthContext, proxy, proxyAuthContext))
                .setProxy(proxy);
        final boolean useSystemProperties = ConfigUtils.getBoolean(
                session, false, USE_SYSTEM_PROPERTIES + "." + repository.getId(), USE_SYSTEM_PROPERTIES);
        if (useSystemProperties) {
            builder.useSystemProperties();
        }

        final boolean reuseConnections = ConfigUtils.getBoolean(
                session,
                ConfigurationProperties.DEFAULT_HTTP_REUSE_CONNECTIONS,
                ConfigurationProperties.HTTP_REUSE_CONNECTIONS + "." + repository.getId(),
                ConfigurationProperties.HTTP_REUSE_CONNECTIONS);
        if (!reuseConnections) {
            builder.setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);
        }

        return builder;
    }

    protected static InetAddress getHttpLocalAddress(RepositorySystemSession session, RemoteRepository repository) {
        String bindAddress =
                ConfigUtils.getString(session, null, BIND_ADDRESS + "." + repository.getId(), BIND_ADDRESS);
        if (bindAddress == null) {
            return null;
        }
        try {
            return InetAddress.getByName(bindAddress);
        } catch (UnknownHostException uhe) {
            throw new IllegalArgumentException(
                    "Given bind address (" + bindAddress + ") cannot be resolved for remote repository " + repository,
                    uhe);
        }
    }

    protected static HttpHost toHost(Proxy proxy) {
        HttpHost host = null;
        if (proxy != null) {
            // in Maven, the proxy.protocol is used for proxy matching against remote repository protocol; no TLS proxy
            // support
            // https://github.com/apache/maven/issues/2519
            // https://github.com/apache/maven-resolver/issues/745
            host = new HttpHost(proxy.getHost(), proxy.getPort());
        }
        return host;
    }

    protected static CredentialsProvider toCredentialsProvider(
            HttpHost server, AuthenticationContext serverAuthCtx, HttpHost proxy, AuthenticationContext proxyAuthCtx) {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        if (serverAuthCtx != null) {
            AuthScope basicScope = new AuthScope(server.getHostName(), AuthScope.ANY_PORT);
            provider.setCredentials(basicScope, newCredentials(serverAuthCtx));
        }
        if (proxy != null && proxyAuthCtx != null) {
            AuthScope proxyScope = new AuthScope(proxy.getHostName(), proxy.getPort());
            provider.setCredentials(proxyScope, newCredentials(proxyAuthCtx));
        }
        return provider;
    }

    protected static Credentials newCredentials(AuthenticationContext authContext) {
        String username = authContext.get(AuthenticationContext.USERNAME);
        if (username == null) {
            return null;
        }
        String password = authContext.get(AuthenticationContext.PASSWORD);
        return new UsernamePasswordCredentials(username, password);
    }

    protected static class ConnMgrConfig {
        private static final String CIPHER_SUITES = "https.cipherSuites";
        private static final String PROTOCOLS = "https.protocols";

        private final SSLContext context;
        private final HostnameVerifier verifier;
        private final String[] cipherSuites;
        private final String[] protocols;
        private final String httpsSecurityMode;
        private final int connectionMaxTtlSeconds;
        private final int maxConnectionsPerRoute;

        protected ConnMgrConfig(
                RepositorySystemSession session,
                AuthenticationContext authContext,
                String httpsSecurityMode,
                int connectionMaxTtlSeconds,
                int maxConnectionsPerRoute) {
            context =
                    (authContext != null) ? authContext.get(AuthenticationContext.SSL_CONTEXT, SSLContext.class) : null;
            verifier = (authContext != null)
                    ? authContext.get(AuthenticationContext.SSL_HOSTNAME_VERIFIER, HostnameVerifier.class)
                    : null;

            cipherSuites = split(get(session, CIPHER_SUITES));
            protocols = split(get(session, PROTOCOLS));
            this.httpsSecurityMode = httpsSecurityMode;
            this.connectionMaxTtlSeconds = connectionMaxTtlSeconds;
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        }

        private static String get(RepositorySystemSession session, String key) {
            String value = ConfigUtils.getString(session, null, "aether.connector." + key, key);
            if (value == null) {
                value = System.getProperty(key);
            }
            return value;
        }

        private static String[] split(String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return value.split(",+");
        }
    }

    protected static HttpClientConnectionManager newConnectionManager(ConnMgrConfig connMgrConfig) {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory());
        int connectionMaxTtlSeconds = ConfigurationProperties.DEFAULT_HTTP_CONNECTION_MAX_TTL;
        int maxConnectionsPerRoute = ConfigurationProperties.DEFAULT_HTTP_MAX_CONNECTIONS_PER_ROUTE;

        if (connMgrConfig == null) {
            registryBuilder.register("https", SSLConnectionSocketFactory.getSystemSocketFactory());
        } else {
            // config present: use provided, if any, or create (depending on httpsSecurityMode)
            connectionMaxTtlSeconds = connMgrConfig.connectionMaxTtlSeconds;
            maxConnectionsPerRoute = connMgrConfig.maxConnectionsPerRoute;
            SSLSocketFactory sslSocketFactory =
                    connMgrConfig.context != null ? connMgrConfig.context.getSocketFactory() : null;
            HostnameVerifier hostnameVerifier = connMgrConfig.verifier;
            if (ConfigurationProperties.HTTPS_SECURITY_MODE_DEFAULT.equals(connMgrConfig.httpsSecurityMode)) {
                if (sslSocketFactory == null) {
                    sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                }
                if (hostnameVerifier == null) {
                    hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();
                }
            } else if (ConfigurationProperties.HTTPS_SECURITY_MODE_INSECURE.equals(connMgrConfig.httpsSecurityMode)) {
                if (sslSocketFactory == null) {
                    try {
                        sslSocketFactory = new SSLContextBuilder()
                                .loadTrustMaterial(null, (chain, auth) -> true)
                                .build()
                                .getSocketFactory();
                    } catch (Exception e) {
                        throw new SSLInitializationException(
                                "Could not configure '" + connMgrConfig.httpsSecurityMode + "' HTTPS security mode", e);
                    }
                }
                if (hostnameVerifier == null) {
                    hostnameVerifier = NoopHostnameVerifier.INSTANCE;
                }
            } else {
                throw new IllegalArgumentException(
                        "Unsupported '" + connMgrConfig.httpsSecurityMode + "' HTTPS security mode.");
            }

            registryBuilder.register(
                    "https",
                    new SSLConnectionSocketFactory(
                            sslSocketFactory, connMgrConfig.protocols, connMgrConfig.cipherSuites, hostnameVerifier));
        }

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(
                new DefaultHttpClientConnectionOperator(
                        registryBuilder.build(), DefaultSchemePortResolver.INSTANCE, SystemDefaultDnsResolver.INSTANCE),
                ManagedHttpClientConnectionFactory.INSTANCE,
                connectionMaxTtlSeconds,
                TimeUnit.SECONDS);
        connMgr.setMaxTotal(maxConnectionsPerRoute * 2);
        connMgr.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        return connMgr;
    }

    protected static class ResolverServiceUnavailableRetryStrategy implements ServiceUnavailableRetryStrategy {
        protected final int retryCount;
        protected final long retryInterval;
        protected final long retryIntervalMax;
        protected final Set<Integer> serviceUnavailableHttpCodes;

        /**
         * Ugly, but forced by HttpClient API {@link ServiceUnavailableRetryStrategy}: the calls for
         * {@link #retryRequest(HttpResponse, int, HttpContext)} and {@link #getRetryInterval()} are done by same
         * thread and are actually done from spot that are very close to each other (almost subsequent calls).
         */
        protected static final ThreadLocal<Long> RETRY_INTERVAL_HOLDER = new ThreadLocal<>();

        protected ResolverServiceUnavailableRetryStrategy(
                int retryCount, long retryInterval, long retryIntervalMax, Set<Integer> serviceUnavailableHttpCodes) {
            if (retryCount < 0) {
                throw new IllegalArgumentException("retryCount must be >= 0");
            }
            if (retryInterval < 0L) {
                throw new IllegalArgumentException("retryInterval must be >= 0");
            }
            if (retryIntervalMax < 0L) {
                throw new IllegalArgumentException("retryIntervalMax must be >= 0");
            }
            this.retryCount = retryCount;
            this.retryInterval = retryInterval;
            this.retryIntervalMax = retryIntervalMax;
            this.serviceUnavailableHttpCodes = requireNonNull(serviceUnavailableHttpCodes);
        }

        @Override
        public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
            final boolean retry = executionCount <= retryCount
                    && (serviceUnavailableHttpCodes.contains(
                            response.getStatusLine().getStatusCode()));
            if (retry) {
                Long retryInterval = retryInterval(response, executionCount, context);
                if (retryInterval != null) {
                    RETRY_INTERVAL_HOLDER.set(retryInterval);
                    return true;
                }
            }
            RETRY_INTERVAL_HOLDER.remove();
            return false;
        }

        /**
         * Calculates retry interval in milliseconds. If {@link HttpHeaders#RETRY_AFTER} header present, it obeys it.
         * Otherwise, it returns {@link this#retryInterval} long value multiplied with {@code executionCount} (starts
         * from 1 and goes 2, 3,...).
         *
         * @return Long representing the retry interval as millis, or {@code null} if the request should be failed.
         */
        protected Long retryInterval(HttpResponse httpResponse, int executionCount, HttpContext httpContext) {
            Long result = null;
            Header header = httpResponse.getFirstHeader(HttpHeaders.RETRY_AFTER);
            if (header != null && header.getValue() != null) {
                String headerValue = header.getValue();
                if (headerValue.contains(":")) { // is date when to retry
                    Date when = DateUtils.parseDate(headerValue); // presumably future
                    if (when != null) {
                        result = Math.max(when.getTime() - System.currentTimeMillis(), 0L);
                    }
                } else {
                    try {
                        result = Long.parseLong(headerValue) * 1000L; // is in seconds
                    } catch (NumberFormatException e) {
                        // fall through
                    }
                }
            }
            if (result == null) {
                result = executionCount * this.retryInterval;
            }
            if (result > retryIntervalMax) {
                return null;
            }
            return result;
        }

        @Override
        public long getRetryInterval() {
            Long ri = RETRY_INTERVAL_HOLDER.get();
            if (ri == null) {
                return 0L;
            }
            RETRY_INTERVAL_HOLDER.remove();
            return ri;
        }
    }
}
