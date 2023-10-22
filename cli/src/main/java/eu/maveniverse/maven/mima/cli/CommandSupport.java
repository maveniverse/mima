package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private void mayDumpEnv(Runtime runtime, Context context) {
        logger.info("MIMA (Runtime '{}' version {})", runtime.name(), runtime.version());
        logger.info("====");
        if (verbose) {
            logger.info("          Maven version {}", runtime.mavenVersion());
            logger.info("                Managed {}", runtime.managedRepositorySystem());
            logger.info("                Basedir {}", context.basedir());
            logger.info(
                    "                Offline {}",
                    context.repositorySystemSession().isOffline());

            ContextOverrides.MavenSystemHome mavenSystemHome =
                    context.contextOverrides().getMavenSystemHome();
            logger.info("");
            logger.info(
                    "             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
            if (mavenSystemHome != null) {
                logger.info("           settings.xml {}", mavenSystemHome.settingsXml());
                logger.info("         toolchains.xml {}", mavenSystemHome.toolchainsXml());
            }

            ContextOverrides.MavenUserHome mavenUserHome =
                    context.contextOverrides().getMavenUserHome();
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
            logger.info("    Remote repositories");
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
        }
        logger.info("");
    }

    protected ContextOverrides createContextOverrides() {
        // create builder with some sane defaults
        ContextOverrides.Builder builder = ContextOverrides.Builder.create()
                .withUserSettings(true)
                .repositories(null)
                .addRepositories(ContextOverrides.AddRepositories.APPEND);
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
        return builder.build();
    }

    protected Runtime getRuntime() {
        return Runtimes.INSTANCE.getRuntime();
    }

    @Override
    public Integer call() {
        Runtime runtime = getRuntime();
        try (Context context = runtime.create(createContextOverrides())) {
            mayDumpEnv(runtime, context);
            return doCall(context);
        }
    }

    protected abstract Integer doCall(Context context);
}
