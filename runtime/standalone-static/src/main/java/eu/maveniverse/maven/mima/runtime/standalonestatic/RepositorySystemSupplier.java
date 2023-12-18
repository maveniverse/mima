package eu.maveniverse.maven.mima.runtime.standalonestatic;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.MavenArtifactRelocationSource;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.apache.maven.repository.internal.relocation.DistributionManagementArtifactRelocationSource;
import org.apache.maven.repository.internal.relocation.UserPropertiesArtifactRelocationSource;
import org.eclipse.aether.impl.*;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;

/**
 *
 */
public class RepositorySystemSupplier extends org.eclipse.aether.supplier.RepositorySystemSupplier {
    @Override
    protected Map<String, TransporterFactory> getTransporterFactories() {
        Map<String, TransporterFactory> result = super.getTransporterFactories();
        result.put(JdkTransporterFactory.NAME, new JdkTransporterFactory());
        return result;
    }

    protected LinkedHashMap<String, MavenArtifactRelocationSource> getMavenArtifactRelocationSource() {
        // from maven-resolver-provider
        LinkedHashMap<String, MavenArtifactRelocationSource> result = new LinkedHashMap<>();
        result.put(UserPropertiesArtifactRelocationSource.NAME, new UserPropertiesArtifactRelocationSource());
        result.put(
                DistributionManagementArtifactRelocationSource.NAME,
                new DistributionManagementArtifactRelocationSource());
        return result;
    }

    protected ArtifactDescriptorReader getArtifactDescriptorReader(
            RemoteRepositoryManager remoteRepositoryManager,
            VersionResolver versionResolver,
            VersionRangeResolver versionRangeResolver,
            ArtifactResolver artifactResolver,
            ModelBuilder modelBuilder,
            RepositoryEventDispatcher repositoryEventDispatcher,
            ModelCacheFactory modelCacheFactory) {
        // from maven-resolver-provider
        return new DefaultArtifactDescriptorReader(
                remoteRepositoryManager,
                versionResolver,
                versionRangeResolver,
                artifactResolver,
                modelBuilder,
                repositoryEventDispatcher,
                modelCacheFactory,
                getMavenArtifactRelocationSource());
    }
}
