/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.HTTPProxy;
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.helpers.MessageFormatter;
import picocli.CommandLine;

/**
 * Support class.
 */
public abstract class CommandSupport implements Callable<Integer> {
    @CommandLine.Option(
            names = {"-v", "--verbose"},
            description = "Be verbose about things happening")
    protected boolean verbose;

    @CommandLine.Option(
            names = {"-o", "--offline"},
            description = "Work offline")
    protected boolean offline;

    @CommandLine.Option(
            names = {"-s", "--settings"},
            description = "The Maven User Settings file to use")
    protected Path userSettingsXml;

    @CommandLine.Option(
            names = {"-gs", "--global-settings"},
            description = "The Maven Global Settings file to use")
    protected Path globalSettingsXml;

    @CommandLine.Option(
            names = {"-P", "--activate-profiles"},
            split = ",",
            description = "Comma delimited list of profile IDs to activate (may use '+', '-' and '!' prefix)")
    protected java.util.List<String> profiles;

    @CommandLine.Option(
            names = {"-D", "--define"},
            description = "Define a user property")
    protected List<String> userProperties;

    @CommandLine.Option(
            names = {"--proxy"},
            description = "Define a HTTP proxy (host:port)")
    protected String proxy;

    private static final ConcurrentHashMap<String, ArrayDeque<Object>> EXECUTION_CONTEXT = new ConcurrentHashMap<>();

    protected Object getOrCreate(String key, Supplier<?> supplier) {
        ArrayDeque<Object> deque = EXECUTION_CONTEXT.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (deque.isEmpty()) {
            deque.push(supplier.get());
        }
        return deque.peek();
    }

    protected void push(String key, Object object) {
        ArrayDeque<Object> deque = EXECUTION_CONTEXT.computeIfAbsent(key, k -> new ArrayDeque<>());
        deque.push(object);
    }

    protected Object pop(String key) {
        ArrayDeque<Object> deque = EXECUTION_CONTEXT.get(key);
        if (deque == null || deque.isEmpty()) {
            throw new IllegalStateException("No element to pop");
        }
        return deque.pop();
    }

    protected Object peek(String key) {
        ArrayDeque<Object> deque = EXECUTION_CONTEXT.get(key);
        if (deque == null || deque.isEmpty()) {
            throw new IllegalStateException("No element to peek");
        }
        return deque.peek();
    }

    protected void mayDumpEnv(Runtime runtime, Context context, boolean verbose) {
        info("MIMA (Runtime '{}' version {})", runtime.name(), runtime.version());
        info("====");
        info("          Maven version {}", runtime.mavenVersion());
        info("                Managed {}", runtime.managedRepositorySystem());
        info("                Basedir {}", context.basedir());
        info("                Offline {}", context.repositorySystemSession().isOffline());

        MavenSystemHome mavenSystemHome = context.mavenSystemHome();
        info("");
        info("             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
        if (mavenSystemHome != null) {
            info("           settings.xml {}", mavenSystemHome.settingsXml());
            info("         toolchains.xml {}", mavenSystemHome.toolchainsXml());
        }

        MavenUserHome mavenUserHome = context.mavenUserHome();
        info("");
        info("              USER_HOME {}", mavenUserHome.basedir());
        info("           settings.xml {}", mavenUserHome.settingsXml());
        info("  settings-security.xml {}", mavenUserHome.settingsSecurityXml());
        info("       local repository {}", mavenUserHome.localRepository());

        info("");
        info("               PROFILES");
        info("                 Active {}", context.contextOverrides().getActiveProfileIds());
        info("               Inactive {}", context.contextOverrides().getInactiveProfileIds());

        info("");
        info("    REMOTE REPOSITORIES");
        for (RemoteRepository repository : context.remoteRepositories()) {
            if (repository.getMirroredRepositories().isEmpty()) {
                info("                        {}", repository);
            } else {
                info("                        {}, mirror of", repository);
                for (RemoteRepository mirrored : repository.getMirroredRepositories()) {
                    info("                          {}", mirrored);
                }
            }
        }

        if (context.httpProxy() != null) {
            HTTPProxy proxy = context.httpProxy();
            info("");
            info("             HTTP PROXY");
            info("                    url {}://{}:{}", proxy.getProtocol(), proxy.getHost(), proxy.getPort());
            info("          nonProxyHosts {}", proxy.getNonProxyHosts());
        }

        if (verbose) {
            info("");
            info("        USER PROPERTIES");
            context.contextOverrides().getUserProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> info("                         {}={}", e.getKey(), e.getValue()));
            info("      SYSTEM PROPERTIES");
            context.contextOverrides().getSystemProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> info("                         {}={}", e.getKey(), e.getValue()));
            info("      CONFIG PROPERTIES");
            context.contextOverrides().getConfigProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> info("                         {}={}", e.getKey(), e.getValue()));
        }
        info("");
    }

    protected Runtime getRuntime() {
        return (Runtime) getOrCreate(Runtime.class.getName(), Runtimes.INSTANCE::getRuntime);
    }

    protected ContextOverrides getContextOverrides() {
        return (ContextOverrides) getOrCreate(ContextOverrides.class.getName(), () -> {
            // create builder with some sane defaults
            ContextOverrides.Builder builder = ContextOverrides.create().withUserSettings(true);
            if (offline) {
                builder.offline(true);
            }
            if (userSettingsXml != null) {
                builder.withUserSettingsXmlOverride(userSettingsXml);
            }
            if (globalSettingsXml != null) {
                builder.withGlobalSettingsXmlOverride(globalSettingsXml);
            }
            if (profiles != null && !profiles.isEmpty()) {
                ArrayList<String> activeProfiles = new ArrayList<>();
                ArrayList<String> inactiveProfiles = new ArrayList<>();
                for (String profile : profiles) {
                    if (profile.startsWith("+")) {
                        activeProfiles.add(profile.substring(1));
                    } else if (profile.startsWith("-") || profile.startsWith("!")) {
                        inactiveProfiles.add(profile.substring(1));
                    } else {
                        activeProfiles.add(profile);
                    }
                }
                builder.withActiveProfileIds(activeProfiles).withInactiveProfileIds(inactiveProfiles);
            }
            if (userProperties != null && !userProperties.isEmpty()) {
                HashMap<String, String> defined = new HashMap<>(userProperties.size());
                String name;
                String value;
                for (String property : userProperties) {
                    int i = property.indexOf('=');
                    if (i <= 0) {
                        name = property.trim();
                        value = Boolean.TRUE.toString();
                    } else {
                        name = property.substring(0, i).trim();
                        value = property.substring(i + 1);
                    }
                    defined.put(name, value);
                }
                builder.userProperties(defined);
            }
            if (proxy != null) {
                String[] elems = proxy.split(":");
                if (elems.length != 2) {
                    throw new IllegalArgumentException("Proxy must be specified as 'host:port'");
                }
                Proxy proxySettings = new Proxy();
                proxySettings.setId("mima-mixin");
                proxySettings.setActive(true);
                proxySettings.setProtocol("http");
                proxySettings.setHost(elems[0]);
                proxySettings.setPort(Integer.parseInt(elems[1]));
                Settings proxyMixin = new Settings();
                proxyMixin.addProxy(proxySettings);
                builder.withEffectiveSettingsMixin(proxyMixin);
            }
            return builder.build();
        });
    }

    protected Context getContext() {
        return (Context) getOrCreate(Context.class.getName(), () -> getRuntime().create(getContextOverrides()));
    }

    protected VersionScheme getVersionScheme() {
        return new GenericVersionScheme();
    }

    protected void verbose(String message) {
        log(true, System.out, message);
    }

    protected void verbose(String format, Object arg1) {
        log(true, System.out, MessageFormatter.format(format, arg1).getMessage());
    }

    protected void verbose(String format, Object arg1, Object arg2) {
        log(true, System.out, MessageFormatter.format(format, arg1, arg2).getMessage());
    }

    protected void verbose(String format, Object arg1, Object arg2, Object arg3) {
        log(
                true,
                System.out,
                MessageFormatter.arrayFormat(format, new Object[] {arg1, arg2, arg3})
                        .getMessage());
    }

    protected void info(String message) {
        log(false, System.out, message);
    }

    protected void info(String format, Object arg1) {
        log(false, System.out, MessageFormatter.format(format, arg1).getMessage());
    }

    protected void info(String format, Object arg1, Object arg2) {
        log(false, System.out, MessageFormatter.format(format, arg1, arg2).getMessage());
    }

    protected void info(String format, Object arg1, Object arg2, Object arg3) {
        log(
                false,
                System.out,
                MessageFormatter.arrayFormat(format, new Object[] {arg1, arg2, arg3})
                        .getMessage());
    }

    protected void error(String message, Throwable throwable) {
        log(System.err, failure(message), throwable);
    }

    private void log(boolean verbose, PrintStream ps, String message) {
        if (verbose && !this.verbose) {
            return;
        }
        log(ps, message, null);
    }

    private void log(PrintStream ps, String message, Throwable throwable) {
        ps.println(message);
        writeThrowable(throwable, ps);
    }

    private static String failure(String format) {
        return "\u001b[1;31m" + format + "\u001b[m";
    }

    private static String strong(String format) {
        return "\u001b[1m" + format + "\u001b[m";
    }

    private void writeThrowable(Throwable t, PrintStream stream) {
        if (t == null) {
            return;
        }
        String builder = failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder += ": " + failure(t.getMessage());
        }
        stream.println(builder);

        printStackTrace(t, stream, "");
    }

    private void printStackTrace(Throwable t, PrintStream stream, String prefix) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            builder.append(prefix);
            builder.append("    ");
            builder.append(strong("at"));
            builder.append(" ");
            builder.append(e.getClassName());
            builder.append(".");
            builder.append(e.getMethodName());
            builder.append(" (");
            builder.append(strong(getLocation(e)));
            builder.append(")");
            stream.println(builder);
            builder.setLength(0);
        }
        for (Throwable se : t.getSuppressed()) {
            writeThrowable(se, stream, "Suppressed", prefix + "    ");
        }
        Throwable cause = t.getCause();
        if (cause != null && t != cause) {
            writeThrowable(cause, stream, "Caused by", prefix);
        }
    }

    private void writeThrowable(Throwable t, PrintStream stream, String caption, String prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix)
                .append(strong(caption))
                .append(": ")
                .append(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.append(": ").append(failure(t.getMessage()));
        }
        stream.println(builder);

        printStackTrace(t, stream, prefix);
    }

    protected String getLocation(final StackTraceElement e) {
        assert e != null;

        if (e.isNativeMethod()) {
            return "Native Method";
        } else if (e.getFileName() == null) {
            return "Unknown Source";
        } else if (e.getLineNumber() >= 0) {
            return e.getFileName() + ":" + e.getLineNumber();
        } else {
            return e.getFileName();
        }
    }
}
