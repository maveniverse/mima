package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.request.BooleanQuery.and;
import static org.apache.maven.search.request.FieldQuery.fieldQuery;

import eu.maveniverse.maven.mima.context.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.search.MAVEN;
import org.apache.maven.search.Record;
import org.apache.maven.search.SearchBackend;
import org.apache.maven.search.SearchRequest;
import org.apache.maven.search.SearchResponse;
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory;
import org.apache.maven.search.request.Query;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Identify.
 */
@CommandLine.Command(name = "identify", description = "Identifies Maven Artifacts")
public final class Identify extends CommandSupport {

    @CommandLine.Parameters(index = "0", description = "The sha1 checksum to identify artifact with")
    private String sha1;

    @Override
    protected Integer doCall(Context context) {
        logger.info("Identify {}", sha1);

        try {
            try (SearchBackend backend = new SmoSearchBackendFactory().createDefault()) {
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

    static Query toQuery(Artifact artifact) {
        Query result = fieldQuery(MAVEN.GROUP_ID, artifact.getGroupId());
        result = and(result, fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId()));
        result = and(result, fieldQuery(MAVEN.VERSION, artifact.getVersion()));
        if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
            result = and(result, fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier()));
        }
        result = and(result, fieldQuery(MAVEN.FILE_EXTENSION, artifact.getExtension()));
        return result;
    }

    static List<String> renderPage(AtomicInteger counter, List<Record> page) {
        ArrayList<String> result = new ArrayList<>();
        for (Record record : page) {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getValue(MAVEN.GROUP_ID));
            if (record.hasField(MAVEN.ARTIFACT_ID)) {
                sb.append(":").append(record.getValue(MAVEN.ARTIFACT_ID));
            }
            if (record.hasField(MAVEN.VERSION)) {
                sb.append(":").append(record.getValue(MAVEN.VERSION));
            }
            if (record.hasField(MAVEN.PACKAGING)) {
                if (record.hasField(MAVEN.CLASSIFIER)) {
                    sb.append(":").append(record.getValue(MAVEN.CLASSIFIER));
                }
                sb.append(":").append(record.getValue(MAVEN.PACKAGING));
            } else if (record.hasField(MAVEN.FILE_EXTENSION)) {
                if (record.hasField(MAVEN.CLASSIFIER)) {
                    sb.append(":").append(record.getValue(MAVEN.CLASSIFIER));
                }
                sb.append(":").append(record.getValue(MAVEN.FILE_EXTENSION));
            }

            List<String> remarks = new ArrayList<>();
            if (record.getLastUpdated() != null) {
                remarks.add("lastUpdate=" + Instant.ofEpochMilli(record.getLastUpdated()));
            }
            if (record.hasField(MAVEN.VERSION_COUNT)) {
                remarks.add("versionCount=" + record.getValue(MAVEN.VERSION_COUNT));
            }
            if (record.hasField(MAVEN.HAS_SOURCE)) {
                remarks.add("hasSource=" + record.getValue(MAVEN.HAS_SOURCE));
            }
            if (record.hasField(MAVEN.HAS_JAVADOC)) {
                remarks.add("hasJavadoc=" + record.getValue(MAVEN.HAS_JAVADOC));
            }

            result.add(counter.incrementAndGet() + ". " + sb);
            if (!remarks.isEmpty()) {
                result.add("   " + remarks);
            }
        }
        return result;
    }
}
