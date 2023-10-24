package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.HTTPProxy;
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Support.
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
    protected List<String> profiles;

    @CommandLine.Option(
            names = {"-D", "--define"},
            description = "Define a user property")
    protected List<String> userProperties;

    @CommandLine.Option(
            names = {"--proxy"},
            description = "Define a HTTP proxy (host:port)")
    protected String proxy;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private static final AtomicBoolean VWO = new AtomicBoolean(false);

    protected void writeVersionOnce(Runtime runtime) {
        if (VWO.compareAndSet(false, true)) {
            logger.info("MIMA (Runtime '{}' version {})", runtime.name(), runtime.version());
            logger.info("====");
        }
    }

    protected void mayDumpEnv(Runtime runtime, Context context) {
        writeVersionOnce(runtime);
        logger.info("          Maven version {}", runtime.mavenVersion());
        logger.info("                Managed {}", runtime.managedRepositorySystem());
        logger.info("                Basedir {}", context.basedir());
        logger.info(
                "                Offline {}", context.repositorySystemSession().isOffline());

        MavenSystemHome mavenSystemHome = context.mavenSystemHome();
        logger.info("");
        logger.info("             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
        if (mavenSystemHome != null) {
            logger.info("           settings.xml {}", mavenSystemHome.settingsXml());
            logger.info("         toolchains.xml {}", mavenSystemHome.toolchainsXml());
        }

        MavenUserHome mavenUserHome = context.mavenUserHome();
        logger.info("");
        logger.info("              USER_HOME {}", mavenUserHome.basedir());
        logger.info("           settings.xml {}", mavenUserHome.settingsXml());
        logger.info("  settings-security.xml {}", mavenUserHome.settingsSecurityXml());
        logger.info("       local repository {}", mavenUserHome.localRepository());

        logger.info("");
        logger.info("               PROFILES");
        logger.info("                 Active {}", context.contextOverrides().getActiveProfileIds());
        logger.info("               Inactive {}", context.contextOverrides().getInactiveProfileIds());

        logger.info("");
        logger.info("    REMOTE REPOSITORIES");
        for (RemoteRepository repository : context.remoteRepositories()) {
            if (repository.getMirroredRepositories().isEmpty()) {
                logger.info("                        {}", repository);
            } else {
                logger.info("                        {}, mirror of", repository);
                for (RemoteRepository mirrored : repository.getMirroredRepositories()) {
                    logger.info("                          {}", mirrored);
                }
            }
        }

        if (context.httpProxy() != null) {
            HTTPProxy proxy = context.httpProxy();
            logger.info("");
            logger.info("             HTTP PROXY");
            logger.info("                    url {}://{}:{}", proxy.getProtocol(), proxy.getHost(), proxy.getPort());
            logger.info("          nonProxyHosts {}", proxy.getNonProxyHosts());
        }

        if (verbose) {
            logger.info("");
            logger.info("        USER PROPERTIES");
            context.contextOverrides().getUserProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> logger.info("                         {}={}", e.getKey(), e.getValue()));
            logger.info("      SYSTEM PROPERTIES");
            context.contextOverrides().getSystemProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> logger.info("                         {}={}", e.getKey(), e.getValue()));
            logger.info("      CONFIG PROPERTIES");
            context.contextOverrides().getConfigProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> logger.info("                         {}={}", e.getKey(), e.getValue()));
        }
        logger.info("");
    }

    protected ContextOverrides createContextOverrides() {
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
    }

    protected Runtime getRuntime() {
        return Runtimes.INSTANCE.getRuntime();
    }

    @Override
    public Integer call() {
        Runtime runtime = getRuntime();
        writeVersionOnce(runtime);
        try (Context context = runtime.create(createContextOverrides())) {
            return doCall(context);
        }
    }

    protected Integer doCall(Context context) {
        throw new RuntimeException("Not implemented; you should override this method in subcommand");
    }
}
