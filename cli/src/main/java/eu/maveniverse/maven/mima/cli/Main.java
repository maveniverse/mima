package eu.maveniverse.maven.mima.cli;

import picocli.CommandLine;

/**
 * Main.
 */
@CommandLine.Command(
        name = "mima",
        subcommands = {Resolve.class},
        version = "1.0",
        description = "MIMA CLI")
public class Main extends CommandSupport {
    @Override
    public Integer call() {
        logger.info("Hello!");
        return 1;
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
