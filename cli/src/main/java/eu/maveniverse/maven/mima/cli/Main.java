package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
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
        version = "1.0",
        description = "MIMA CLI")
public class Main extends CommandSupport {
    @Override
    public Integer call() {
        try (Context context = getContext()) {
            verbose = true;
            mayDumpEnv(getRuntime(), context);
        }
        return 0;
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
