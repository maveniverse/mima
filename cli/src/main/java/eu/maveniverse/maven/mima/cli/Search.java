package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.Query.query;

import eu.maveniverse.maven.mima.context.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Search.
 */
@CommandLine.Command(name = "search", description = "Searches Maven Artifacts")
public final class Search extends SearchSupport {

    @CommandLine.Parameters(index = "0", description = "The expression to search for")
    private String expression;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Search {}", expression);

        try {
            try (SearchBackend backend = getSmoBackend(repositoryId)) {
                Query query;
                try {
                    query = toSmoQuery(new DefaultArtifact(expression));
                } catch (IllegalArgumentException e) {
                    query = query(expression);
                }
                SearchRequest searchRequest = new SearchRequest(query);
                SearchResponse searchResponse = backend.search(searchRequest);
                logger.info("");
                AtomicInteger counter = new AtomicInteger();
                renderPage(counter, searchResponse.getPage()).forEach(logger::info);
                while (searchResponse.getCurrentHits() > 0) {
                    searchResponse =
                            backend.search(searchResponse.getSearchRequest().nextPage());
                    renderPage(counter, searchResponse.getPage()).forEach(logger::info);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return 0;
    }
}
