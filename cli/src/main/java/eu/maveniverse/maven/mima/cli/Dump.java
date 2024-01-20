package eu.maveniverse.maven.mima.cli;

import picocli.CommandLine;

/**
 * Dumps MIMA environment.
 */
@CommandLine.Command(name = "dump", description = "Dump MIMA environment")
public final class Dump extends CommandSupport {
    @Override
    public Integer call() {
        mayDumpEnv(getRuntime(), getContext(), true);
        return 0;
    }
}
