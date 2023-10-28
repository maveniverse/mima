package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import eu.maveniverse.maven.mima.context.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import picocli.CommandLine;

/**
 * Identify.
 */
@CommandLine.Command(name = "identify", description = "Identifies Maven Artifacts")
public final class Identify extends SearchSupport {

    @CommandLine.Parameters(index = "0", description = "The sha1 checksum to identify artifact with")
    private String sha1;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Identify {}", sha1);

        try {
            try (SearchBackend backend = getSmoBackend(repositoryId)) {
                SearchRequest searchRequest = new SearchRequest(fieldQuery(MAVEN.SHA1, sha1));
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
