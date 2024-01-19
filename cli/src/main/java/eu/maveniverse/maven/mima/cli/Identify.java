package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    @CommandLine.Parameters(index = "0", description = "File or sha1 checksum to identify artifact with")
    private String target;

    @Override
    protected Integer doCall() throws IOException {
        String sha1;
        if (Files.exists(Paths.get(target))) {
            byte[] fileContent = Files.readAllBytes(Paths.get(target));
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Not able to calculate SHA1", e);
            }
            byte[] digest = md.digest(fileContent);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            }
            sha1 = sb.toString();
        } else {
            sha1 = target;
        }

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
