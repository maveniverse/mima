package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.Runtime;
import picocli.CommandLine;

/**
 * Main.
 */
@CommandLine.Command(
        name = "mima",
        subcommands = {Classpath.class, Deploy.class, Install.class, Repl.class, Resolve.class},
        version = "1.0",
        description = "MIMA CLI")
public class Main extends CommandSupport {
    @Override
    public Integer call() {
        Runtime runtime = getRuntime();
        try (Context context = runtime.create(createContextOverrides())) {
            verbose = true;
            mayDumpEnv(runtime, context);
        }
        return 1;
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
