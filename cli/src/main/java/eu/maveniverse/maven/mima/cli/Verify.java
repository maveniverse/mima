package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import eu.maveniverse.maven.mima.context.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Verify.
 */
@CommandLine.Command(name = "verify", description = "Verifies Maven Artifact")
public final class Verify extends SearchSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to check")
    private String gav;

    @CommandLine.Parameters(index = "1", description = "The known SHA-1 of GAV")
    private String sha1;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Verify {}", gav);

        try {
            try (SearchBackend backend =
                    getRemoteRepositoryBackend(repositoryId, repositoryBaseUri, repositoryVendor)) {
                Artifact artifact = new DefaultArtifact(gav);
                boolean verified = verify(backend, new DefaultArtifact(gav), sha1);
                logger.info("");
                logger.info("Artifact SHA1({})={}: {}", artifact, sha1, verified ? "MATCHED" : "NOT MATCHED");
                return verified ? 0 : 1;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean verify(SearchBackend backend, Artifact artifact, String sha1) throws IOException {
        Query query = toRrQuery(artifact);
        query = and(query, fieldQuery(MAVEN.SHA1, sha1));
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        return searchResponse.getTotalHits() == 1;
    }
}
