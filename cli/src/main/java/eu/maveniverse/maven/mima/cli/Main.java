package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import picocli.CommandLine;

/**
 * Main.
 */
@CommandLine.Command(
        name = "mima",
        subcommands = {Resolve.class, Install.class, Deploy.class},
        version = "1.0",
        description = "MIMA CLI")
public class Main extends CommandSupport {
    @Override
    protected Integer doCall(Context context) {
        logger.info("Hello from MIMA!");
        return 1;
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
