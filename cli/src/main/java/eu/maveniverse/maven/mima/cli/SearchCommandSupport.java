package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.api.transport.Java11HttpClientTransport;
import org.apache.maven.search.api.transport.Transport;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackendFactory;
import org.apache.maven.search.backend.remoterepository.ResponseExtractor;
import org.apache.maven.search.backend.remoterepository.extractor.MavenCentralResponseExtractor;
import org.apache.maven.search.backend.remoterepository.extractor.Nx2ResponseExtractor;
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Search support.
 */
public abstract class SearchCommandSupport extends CommandSupport {
    @CommandLine.Option(
            names = {"--repositoryId"},
            defaultValue = "central",
            description = "The targeted repository ID")
    protected String repositoryId;

    @CommandLine.Option(
            names = {"--repositoryBaseUri"},
            defaultValue = "https://repo.maven.apache.org/maven2/",
            description = "The targeted repository base Uri")
    protected String repositoryBaseUri;

    @CommandLine.Option(
            names = {"--repositoryVendor"},
            defaultValue = "central",
            description = "The targeted repository vendor")
    protected String repositoryVendor;

    protected Transport getTransport() {
        return (Transport) getOrCreate(Transport.class.getName(), Java11HttpClientTransport::new);
    }

    protected SearchBackend getRemoteRepositoryBackend(String repositoryId, String baseUri, String vendor) {
        final ResponseExtractor extractor;
        if ("central".equals(vendor)) {
            extractor = new MavenCentralResponseExtractor();
        } else if ("nx2".equals(vendor)) {
            extractor = new Nx2ResponseExtractor();
        } else {
            throw new IllegalArgumentException("Unknown remote vendor");
        }
        return (SearchBackend) getOrCreate(
                SearchBackend.class.getName() + "-" + repositoryId,
                () -> RemoteRepositorySearchBackendFactory.create(
                        repositoryId + "-rr", repositoryId, baseUri, getTransport(), extractor));
    }

    protected SearchBackend getSmoBackend(String repositoryId) {
        if (!"central".equals(repositoryId)) {
            throw new IllegalArgumentException("The SMO service is offered for Central only");
        }
        return SmoSearchBackendFactory.create(
                repositoryId + "-smo", repositoryId, "https://search.maven.org/solrsearch/select", getTransport());
    }

    /**
     * Query out of {@link Artifact} for RR backend: it maps all that are given.
     */
    protected Query toRrQuery(Artifact artifact) {
        Query result = fieldQuery(MAVEN.GROUP_ID, artifact.getGroupId());
        result = and(result, fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId()));
        result = and(result, fieldQuery(MAVEN.VERSION, artifact.getVersion()));
        if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
            result = and(result, fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier()));
        }
        result = and(result, fieldQuery(MAVEN.FILE_EXTENSION, artifact.getExtension()));

        return result;
    }

    /**
     * Query out of {@link Artifact} for SMO backend: SMO "have no idea" what file extension is, it handles only
     * "packaging", so we map here {@link Artifact#getExtension()} into "packaging" instead. Also, if we query
     * fields with value "*" SMO throws HTTP 400, so we simply omit "*" from queries, but they still allow us to
     * enter "*:*:1.0" that translates to "version=1.0" query.
     */
    protected Query toSmoQuery(Artifact artifact) {
        Query result = null;
        if (!"*".equals(artifact.getGroupId())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.GROUP_ID, artifact.getGroupId()))
                    : fieldQuery(MAVEN.GROUP_ID, artifact.getGroupId());
        }
        if (!"*".equals(artifact.getArtifactId())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId()))
                    : fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId());
        }
        if (!"*".equals(artifact.getVersion())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.VERSION, artifact.getVersion()))
                    : fieldQuery(MAVEN.VERSION, artifact.getVersion());
        }
        if (!"*".equals(artifact.getClassifier()) && !"".equals(artifact.getClassifier())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier()))
                    : fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier());
        }
        if (!"*".equals(artifact.getExtension())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.PACKAGING, artifact.getExtension()))
                    : fieldQuery(MAVEN.PACKAGING, artifact.getExtension());
        }

        if (result == null) {
            throw new IllegalArgumentException("Too broad query expression");
        }
        return result;
    }

    protected java.util.List<String> renderPage(AtomicInteger counter, java.util.List<Record> page) {
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

    @Override
    public final Integer call() {
        try {
            return doCall();
        } catch (Exception e) {
            error("Error", e);
            return 1;
        }
    }

    protected Integer doCall() throws Exception {
        throw new RuntimeException("Not implemented; you should override this method in subcommand");
    }
}
