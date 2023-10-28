package eu.maveniverse.maven.mima.cli;

import java.io.IOException;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.artifact.SubArtifact;
import picocli.CommandLine;

/**
 * Exists.
 */
@CommandLine.Command(name = "exists", description = "Checks Maven Artifact existence")
public final class Exists extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to check")
    private String gav;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Download sources JARs as well (best effort)")
    private boolean sources;

    @CommandLine.Option(
            names = {"--javadoc"},
            description = "Download javadoc JARs as well (best effort)")
    private boolean javadoc;

    @Override
    protected Integer doCall() throws IOException {
        logger.info("Exists {}", gav);

        try (SearchBackend backend = getRemoteRepositoryBackend(repositoryId, repositoryBaseUri, repositoryVendor)) {
            Artifact artifact = new DefaultArtifact(gav);
            boolean exists = exists(backend, artifact);
            logger.info("");
            logger.info("Artifact {} {}", artifact, exists ? "EXISTS" : "NOT EXISTS");
            if (sources) {
                Artifact sources = new SubArtifact(artifact, "sources", "jar");
                logger.info("    {} {}", sources, exists(backend, sources) ? "EXISTS" : "NOT EXISTS");
            }
            if (javadoc) {
                Artifact javadoc = new SubArtifact(artifact, "javadoc", "jar");
                logger.info("    {} {}", javadoc, exists(backend, javadoc) ? "EXISTS" : "NOT EXISTS");
            }
            return exists ? 0 : 1;
        }
    }

    private boolean exists(SearchBackend backend, Artifact artifact) throws IOException {
        Query query = toRrQuery(artifact);
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        return searchResponse.getTotalHits() == 1;
    }
}
