package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import java.io.IOException;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import picocli.CommandLine;

/**
 * Identify.
 */
@CommandLine.Command(name = "identify", description = "Identifies Maven Artifacts")
public final class Identify extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The sha1 checksum to identify artifact with")
    private String sha1;

    @Override
    protected Integer doCall() throws IOException {
        try (SearchBackend backend = getSmoBackend(repositoryId)) {
            SearchRequest searchRequest = new SearchRequest(fieldQuery(MAVEN.SHA1, sha1));
            SearchResponse searchResponse = backend.search(searchRequest);

            renderPage(searchResponse.getPage()).forEach(this::info);
            while (searchResponse.getCurrentHits() > 0) {
                searchResponse =
                        backend.search(searchResponse.getSearchRequest().nextPage());
                renderPage(searchResponse.getPage()).forEach(this::info);
            }
        }
        return 0;
    }
}
