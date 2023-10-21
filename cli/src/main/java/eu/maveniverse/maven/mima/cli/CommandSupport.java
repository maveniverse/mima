package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
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
            names = {"-s", "--settings"},
            description = "The Maven User Settings file to use")
    protected Path userSettingsXml;

    @CommandLine.Option(
            names = {"-gs", "--global-settings"},
            description = "The Maven Global Settings file to use")
    protected Path globalSettingsXml;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected void mayDumpEnv(Runtime runtime, Context context) {
        logger.info("MIMA Runtime {} {}", runtime.name(), runtime.version());
        logger.info("==========================================================");
        if (verbose) {
            logger.info("          Maven version {}", runtime.mavenVersion());
            logger.info("                Managed {}", runtime.managedRepositorySystem());
            logger.info("                Basedir {}", context.basedir());
            logger.info("");
            ContextOverrides.MavenSystemHome mavenSystemHome =
                    context.contextOverrides().getMavenSystemHome();
            logger.info(
                    "             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
            ContextOverrides.MavenUserHome mavenUserHome =
                    context.contextOverrides().getMavenUserHome();
            logger.info("");
            logger.info("              USER_HOME {}", mavenUserHome.basedir());
            logger.info("           settings.xml {}", mavenUserHome.settingsXml());
            logger.info("  settings-security.xml {}", mavenUserHome.settingsSecurityXml());
            logger.info("       local repository {}", mavenUserHome.localRepository());
            logger.info("");
            logger.info("    Remote repositories");
            for (RemoteRepository repository : context.remoteRepositories()) {
                logger.info("                        {}", repository);
            }
        }
        logger.info("");
    }

    protected void doWithContext(Consumer<Context> contextConsumer) {
        // create builder with some sane defaults
        ContextOverrides.Builder builder = ContextOverrides.Builder.create()
                .withUserSettings(true)
                .repositories(null)
                .addRepositories(ContextOverrides.AddRepositories.APPEND);
        if (userSettingsXml != null) {
            builder.withUserSettingsXmlOverride(userSettingsXml);
        }
        if (globalSettingsXml != null) {
            builder.withGlobalSettingsXmlOverride(globalSettingsXml);
        }
        ContextOverrides contextOverrides = builder.build();
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(contextOverrides)) {
            mayDumpEnv(runtime, context);
            contextConsumer.accept(context);
        }
    }
}
