package eu.maveniverse.maven.mima.runtime.maven;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.RuntimeSupport;
import eu.maveniverse.maven.mima.context.RuntimeVersions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named
public class MavenRuntime extends RuntimeSupport {
    private static final Map<String, String> M2R;

    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("4.0", "1.9.4");
        map.put("3.9", "1.9.4");
        map.put("3.8", "1.6.3");
        map.put("3.6", "1.4.1");
        map.put("3.5", "1.1.1");
        map.put("3.3", "1.0.2.v20150114");
        map.put("3.2", "1.0.0.v20140518");
        map.put("3.1", "0.9.0.M2");
        M2R = Collections.unmodifiableMap(map);
    }

    private final RepositorySystem repositorySystem;

    private final MavenSession mavenSession;

    private final RuntimeVersions runtimeVersions;

    @Inject
    public MavenRuntime(RepositorySystem repositorySystem, MavenSession mavenSession, RuntimeInformation rt) {
        super("embedded-maven", 10);
        this.repositorySystem = repositorySystem;
        this.mavenSession = mavenSession;
        this.runtimeVersions = runtimeVersions(rt.getMavenVersion());
    }

    private RuntimeVersions runtimeVersions(String mavenVersion) {
        if (mavenVersion == null) {
            return new RuntimeVersions(RuntimeVersions.UNKNOWN, RuntimeVersions.UNKNOWN);
        }
        if (mavenVersion.length() < 3) {
            return new RuntimeVersions(RuntimeVersions.UNKNOWN, mavenVersion);
        }
        String majorMinor = mavenVersion.substring(0, 3);
        String resolverVersion = M2R.get(majorMinor);
        if (resolverVersion != null) {
            return new RuntimeVersions(resolverVersion, mavenVersion);
        } else {
            return new RuntimeVersions(RuntimeVersions.UNKNOWN, mavenVersion);
        }
    }

    @Override
    public boolean managedRepositorySystem() {
        return false;
    }

    @Override
    public RuntimeVersions runtimeVersions() {
        return runtimeVersions;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        return customizeContext(
                this,
                overrides,
                new Context(
                        this,
                        repositorySystem,
                        mavenSession.getRepositorySession(),
                        mavenSession.getCurrentProject().getRemoteProjectRepositories(),
                        null),
                false); // unmanaged context: close should NOT shut down repositorySystem
    }
}
