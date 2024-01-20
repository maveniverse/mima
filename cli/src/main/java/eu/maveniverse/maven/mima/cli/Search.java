package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.Query.query;

import java.io.IOException;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Searches artifacts using SMO service.
 */
@CommandLine.Command(name = "search", description = "Searches Maven Artifacts")
public final class Search extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The expression to search for")
    private String expression;

    @Override
    protected Integer doCall() throws IOException {
        try (SearchBackend backend = getSmoBackend(repositoryId)) {
            Query query;
            try {
                query = toSmoQuery(new DefaultArtifact(expression));
            } catch (IllegalArgumentException e) {
                query = query(expression);
            }
            SearchRequest searchRequest = new SearchRequest(query);
            SearchResponse searchResponse = backend.search(searchRequest);

            renderPage(searchResponse.getPage(), null).forEach(this::info);
            while (searchResponse.getCurrentHits() > 0) {
                searchResponse =
                        backend.search(searchResponse.getSearchRequest().nextPage());
                renderPage(searchResponse.getPage(), null).forEach(this::info);
            }
        }
        return 0;
    }
}
