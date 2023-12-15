package eu.maveniverse.maven.mima.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    @CommandLine.Parameters(description = "The GAV to check")
    private List<String> gavs;

    @CommandLine.Option(
            names = {"--pom"},
            description = "Check POM presence as well")
    private boolean pom;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Check sources JARs as well")
    private boolean sources;

    @CommandLine.Option(
            names = {"--javadoc"},
            description = "Check javadoc JARs as well")
    private boolean javadoc;

    @CommandLine.Option(
            names = {"--all-required"},
            description =
                    "If set, missing sources or javadoc will be reported as failure (otherwise just the given GAVs presence are required)")
    private boolean allRequired;

    @Override
    protected Integer doCall() throws IOException {
        info("Exists {}", gavs);
        info("");

        ArrayList<Artifact> missingOnes = new ArrayList<>();
        ArrayList<Artifact> existingOnes = new ArrayList<>();
        try (SearchBackend backend = getRemoteRepositoryBackend(repositoryId, repositoryBaseUri, repositoryVendor)) {
            for (String gav : gavs) {
                Artifact artifact = new DefaultArtifact(gav);
                boolean exists = exists(backend, artifact);
                if (!exists) {
                    missingOnes.add(artifact);
                } else {
                    existingOnes.add(artifact);
                }
                info("Artifact {} {}", artifact, exists ? "EXISTS" : "NOT EXISTS");
                if (pom && !"pom".equals(artifact.getExtension())) {
                    Artifact pom = new SubArtifact(artifact, null, "pom");
                    exists = exists(backend, pom);
                    if (!exists && allRequired) {
                        missingOnes.add(pom);
                    } else if (allRequired) {
                        existingOnes.add(pom);
                    }
                    info("    {} {}", pom, exists ? "EXISTS" : "NOT EXISTS");
                }
                if (sources) {
                    Artifact sources = new SubArtifact(artifact, "sources", "jar");
                    exists = exists(backend, sources);
                    if (!exists && allRequired) {
                        missingOnes.add(sources);
                    } else if (allRequired) {
                        existingOnes.add(sources);
                    }
                    info("    {} {}", sources, exists ? "EXISTS" : "NOT EXISTS");
                }
                if (javadoc) {
                    Artifact javadoc = new SubArtifact(artifact, "javadoc", "jar");
                    exists = exists(backend, javadoc);
                    if (!exists && allRequired) {
                        missingOnes.add(javadoc);
                    } else if (allRequired) {
                        existingOnes.add(javadoc);
                    }
                    info("    {} {}", javadoc, exists ? "EXISTS" : "NOT EXISTS");
                }
            }
        }
        info("");
        info(
                "Checked TOTAL of {} (existing: {} not existing: {})",
                existingOnes.size() + missingOnes.size(),
                existingOnes.size(),
                missingOnes.size());
        return missingOnes.isEmpty() ? 0 : 1;
    }

    private boolean exists(SearchBackend backend, Artifact artifact) throws IOException {
        Query query = toRrQuery(artifact);
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        return searchResponse.getTotalHits() == 1;
    }
}
