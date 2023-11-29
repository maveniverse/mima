package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import picocli.CommandLine;

/**
 * Record.
 */
@CommandLine.Command(name = "record", description = "Records resolved Maven Artifacts")
public final class Record extends ResolverCommandSupport {

    @Override
    protected Integer doCall(Context context) throws DependencyResolutionException {
        logger.info("Recording...");

        ArtifactRecorder recorder = new ArtifactRecorder();
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(getRepositorySystemSession());
        session.setRepositoryListener(
                session.getRepositoryListener() != null
                        ? ChainedRepositoryListener.newInstance(session.getRepositoryListener(), recorder)
                        : recorder);
        push(ArtifactRecorder.class.getName(), recorder);
        push(RepositorySystemSession.class.getName(), session);

        logger.info("");
        return 0;
    }
}
