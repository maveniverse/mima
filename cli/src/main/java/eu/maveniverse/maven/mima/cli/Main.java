package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.Runtime;
import picocli.CommandLine;

/**
 * Main.
 */
@CommandLine.Command(
        name = "mima",
        subcommands = {
            Classpath.class,
            Deploy.class,
            DeployRecorded.class,
            Dump.class,
            Exists.class,
            Graph.class,
            Identify.class,
            Install.class,
            List.class,
            Search.class,
            Record.class,
            Repl.class,
            Resolve.class,
            Verify.class
        },
        versionProvider = Main.class,
        description = "MIMA CLI",
        mixinStandardHelpOptions = true)
public class Main extends CommandSupport implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        Runtime runtime = getRuntime();
        return new String[] {"MIMA " + runtime.version()};
    }

    @Override
    public Integer call() {
        try (Context context = getContext()) {
            mayDumpEnv(getRuntime(), context, false);
            new Repl().call();
        }
        return 0;
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
