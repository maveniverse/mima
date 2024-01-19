package eu.maveniverse.maven.mima.cli;

import picocli.CommandLine;

/**
 * Dump.
 */
@CommandLine.Command(name = "dump", description = "Dump MIMA environment")
public final class Dump extends CommandSupport {
    @Override
    public Integer call() {
        mayDumpEnv(getRuntime(), getContext(), true);
        return 0;
    }
}
