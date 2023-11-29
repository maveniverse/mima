package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import picocli.CommandLine;

/**
 * List.
 */
@CommandLine.Command(name = "list", description = "Lists Maven Artifacts")
public final class List extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV-oid to list (G or G:A or G:A:V)")
    private String gavoid;

    @Override
    protected Integer doCall() throws IOException {
        logger.info("List {}", gavoid);

        try (SearchBackend backend = getRemoteRepositoryBackend(repositoryId, repositoryBaseUri, repositoryVendor)) {
            String[] elements = gavoid.split(":");
            if (elements.length < 1 || elements.length > 3) {
                throw new IllegalArgumentException("Invalid gavoid");
            }

            Query query = fieldQuery(MAVEN.GROUP_ID, elements[0]);
            if (elements.length > 1) {
                query = and(query, fieldQuery(MAVEN.ARTIFACT_ID, elements[1]));
            }
            if (elements.length > 2) {
                query = and(query, fieldQuery(MAVEN.VERSION, elements[2]));
            }
            SearchRequest searchRequest = new SearchRequest(query);
            SearchResponse searchResponse = backend.search(searchRequest);
            logger.info("");
            AtomicInteger counter = new AtomicInteger();
            renderPage(counter, searchResponse.getPage()).forEach(logger::info);
        }
        return 0;
    }
}
