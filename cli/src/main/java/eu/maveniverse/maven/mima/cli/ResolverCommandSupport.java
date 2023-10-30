package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Support.
 */
public abstract class ResolverCommandSupport extends CommandSupport {

    protected RepositorySystemSession getRepositorySystemSession() {
        return (RepositorySystemSession) getOrCreate(
                RepositorySystemSession.class.getName(), () -> getContext().repositorySystemSession());
    }

    protected RemoteRepository buildRemoteRepositoryFromSpec(String remoteRepositorySpec) {
        String[] parts = remoteRepositorySpec.split("::");
        if (parts.length == 1) {
            return new RemoteRepository.Builder("mima", "default", parts[0]).build();
        } else if (parts.length == 2) {
            return new RemoteRepository.Builder(parts[0], "default", parts[1]).build();
        } else {
            throw new IllegalArgumentException("Invalid remote repository spec");
        }
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
