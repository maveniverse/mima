package eu.maveniverse.maven.mima.cli;

import java.util.concurrent.Callable;
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

    protected Logger logger = LoggerFactory.getLogger(getClass());
}
