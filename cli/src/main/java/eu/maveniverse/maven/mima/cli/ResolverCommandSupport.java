package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Support.
 */
public abstract class ResolverCommandSupport extends CommandSupport {

    protected RepositorySystemSession getRepositorySystemSession() {
        return (RepositorySystemSession) getOrCreate(
                RepositorySystemSession.class.getName(), () -> getContext().repositorySystemSession());
    }

    @Override
    public final Integer call() {
        try (Context context = getContext()) {
            return doCall(context);
        } catch (Exception e) {
            logger.error("Error", e);
            return 1;
        }
    }

    protected Integer doCall(Context context) throws Exception {
        throw new RuntimeException("Not implemented; you should override this method in subcommand");
    }
}
