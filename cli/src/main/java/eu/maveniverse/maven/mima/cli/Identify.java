package eu.maveniverse.maven.mima.cli;

import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.eclipse.aether.util.ChecksumUtils;
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
            try {
                MessageDigest sha1md = MessageDigest.getInstance("SHA-1");
                byte[] buf = new byte[8192];
                int read;
                try (FileInputStream fis = new FileInputStream(target)) {
                    read = fis.read(buf);
                    while (read != -1) {
                        sha1md.update(buf, 0, read);
                        read = fis.read(buf);
                    }
                }
                sha1 = ChecksumUtils.toHexString(sha1md.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA1 MessageDigest unavailable", e);
            }
        } else {
            sha1 = target;
        }
        verbose("Identifying artifact with SHA1={}", sha1);
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
