package eu.maveniverse.maven.mima.cli;

import static eu.maveniverse.maven.mima.cli.Identify.renderPage;
import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import eu.maveniverse.maven.mima.context.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackendFactory;
import picocli.CommandLine;

/**
 * List.
 */
@CommandLine.Command(name = "list", description = "Lists Maven Artifacts")
public final class List extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV-oid to list (G or G:A or G:A:V)")
    private String gavoid;

    @Override
    protected Integer doCall(Context context) {
        logger.info("List {}", gavoid);

        try {
            try (SearchBackend backend = RemoteRepositorySearchBackendFactory.createDefaultMavenCentral()) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return 0;
    }
}
