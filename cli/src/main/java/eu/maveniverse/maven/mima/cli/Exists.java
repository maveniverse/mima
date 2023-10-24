package eu.maveniverse.maven.mima.cli;

import static eu.maveniverse.maven.mima.cli.Identify.toQuery;

import eu.maveniverse.maven.mima.context.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.maven.search.SearchBackend;
import org.apache.maven.search.SearchRequest;
import org.apache.maven.search.SearchResponse;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackendFactory;
import org.apache.maven.search.request.Query;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * List.
 */
@CommandLine.Command(name = "exists", description = "Checks Maven Artifact existence")
public final class Exists extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to check")
    private String gav;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Exists {}", gav);

        try {
            try (SearchBackend backend = RemoteRepositorySearchBackendFactory.createDefaultMavenCentral()) {
                Query query = toQuery(new DefaultArtifact(gav));
                SearchRequest searchRequest = new SearchRequest(query);
                SearchResponse searchResponse = backend.search(searchRequest);
                boolean exists = searchResponse.getTotalHits() == 1;
                logger.info("");
                logger.info("It {}", exists ? "EXISTS" : "NOT EXISTS");
                return exists ? 0 : 1;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
