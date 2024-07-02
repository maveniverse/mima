package eu.maveniverse.maven.mima.runtime.standalonestatic;

import eu.maveniverse.maven.mima.context.Lookup;
import java.util.*;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.*;
import org.eclipse.aether.internal.impl.LocalPathComposer;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.TrackingFileManager;
import org.eclipse.aether.internal.impl.collect.DependencyCollectorDelegate;
import org.eclipse.aether.internal.impl.synccontext.named.NameMapper;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactory;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.spi.artifact.ArtifactPredicateFactory;
import org.eclipse.aether.spi.artifact.decorator.ArtifactDecoratorFactory;
import org.eclipse.aether.spi.artifact.generator.ArtifactGeneratorFactory;
import org.eclipse.aether.spi.checksums.ProvidedChecksumsSource;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractorStrategy;
import org.eclipse.aether.spi.io.ChecksumProcessor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.version.VersionScheme;

public class MemoizingRepositorySystemSupplierLookup extends RepositorySystemSupplier implements Lookup {
    private final HashMap<Class<?>, Object> singulars = new HashMap<>();

    private final HashMap<Class<?>, Map<String, Object>> plurals = new HashMap<>();

    public MemoizingRepositorySystemSupplierLookup() {
        memoize(RepositorySystem.class, super.get()); // to trigger filling up of memoized components
    }

    @Override
    public RepositorySystem get() {
        return lookup(RepositorySystem.class).orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    protected <T> T memoize(Class<T> key, T instance) {
        singulars.put(key, instance);
        return instance;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T> Map<String, T> memoize(Class<T> key, Map<String, T> instances) {
        plurals.put(key, (Map) instances);
        return instances;
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        return lookup(type, "default");
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        return Optional.ofNullable(lookupMap(type).get(name));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Map<String, T> lookupMap(Class<T> type) {
        Map<String, T> result = (Map) plurals.get(type);
        if (result != null) {
            return result;
        }
        T component = (T) singulars.get(type);
        if (component != null) {
            return Collections.singletonMap("default", component);
        }
        return Collections.emptyMap();
    }

    @Override
    protected PathProcessor createPathProcessor() {
        return memoize(PathProcessor.class, super.createPathProcessor());
    }

    @Override
    protected TrackingFileManager createTrackingFileManager() {
        return memoize(TrackingFileManager.class, super.createTrackingFileManager());
    }

    @Override
    protected LocalPathComposer createLocalPathComposer() {
        return memoize(LocalPathComposer.class, super.createLocalPathComposer());
    }

    @Override
    protected LocalPathPrefixComposerFactory createLocalPathPrefixComposerFactory() {
        return memoize(LocalPathPrefixComposerFactory.class, super.createLocalPathPrefixComposerFactory());
    }

    @Override
    protected RepositorySystemLifecycle createRepositorySystemLifecycle() {
        return memoize(RepositorySystemLifecycle.class, super.createRepositorySystemLifecycle());
    }

    @Override
    protected OfflineController createOfflineController() {
        return memoize(OfflineController.class, super.createOfflineController());
    }

    @Override
    protected UpdatePolicyAnalyzer createUpdatePolicyAnalyzer() {
        return memoize(UpdatePolicyAnalyzer.class, super.createUpdatePolicyAnalyzer());
    }

    @Override
    protected ChecksumPolicyProvider createChecksumPolicyProvider() {
        return memoize(ChecksumPolicyProvider.class, super.createChecksumPolicyProvider());
    }

    @Override
    protected UpdateCheckManager createUpdateCheckManager() {
        return memoize(UpdateCheckManager.class, super.createUpdateCheckManager());
    }

    @Override
    protected Map<String, NamedLockFactory> createNamedLockFactories() {
        return memoize(NamedLockFactory.class, super.createNamedLockFactories());
    }

    @Override
    protected Map<String, NameMapper> createNameMappers() {
        return memoize(NameMapper.class, super.createNameMappers());
    }

    @Override
    protected NamedLockFactoryAdapterFactory createNamedLockFactoryAdapterFactory() {
        return memoize(NamedLockFactoryAdapterFactory.class, super.createNamedLockFactoryAdapterFactory());
    }

    @Override
    protected SyncContextFactory createSyncContextFactory() {
        return memoize(SyncContextFactory.class, super.createSyncContextFactory());
    }

    @Override
    protected Map<String, ChecksumAlgorithmFactory> createChecksumAlgorithmFactories() {
        return memoize(ChecksumAlgorithmFactory.class, super.createChecksumAlgorithmFactories());
    }

    @Override
    protected ChecksumAlgorithmFactorySelector createChecksumAlgorithmFactorySelector() {
        return memoize(ChecksumAlgorithmFactorySelector.class, super.createChecksumAlgorithmFactorySelector());
    }

    @Override
    protected Map<String, RepositoryLayoutFactory> createRepositoryLayoutFactories() {
        return memoize(RepositoryLayoutFactory.class, super.createRepositoryLayoutFactories());
    }

    @Override
    protected RepositoryLayoutProvider createRepositoryLayoutProvider() {
        return memoize(RepositoryLayoutProvider.class, super.createRepositoryLayoutProvider());
    }

    @Override
    protected LocalRepositoryProvider createLocalRepositoryProvider() {
        return memoize(LocalRepositoryProvider.class, super.createLocalRepositoryProvider());
    }

    @Override
    protected RemoteRepositoryManager createRemoteRepositoryManager() {
        return memoize(RemoteRepositoryManager.class, super.createRemoteRepositoryManager());
    }

    @Override
    protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
        return memoize(RemoteRepositoryFilterSource.class, super.createRemoteRepositoryFilterSources());
    }

    @Override
    protected RemoteRepositoryFilterManager createRemoteRepositoryFilterManager() {
        return memoize(RemoteRepositoryFilterManager.class, super.createRemoteRepositoryFilterManager());
    }

    @Override
    protected Map<String, RepositoryListener> createRepositoryListeners() {
        return memoize(RepositoryListener.class, super.createRepositoryListeners());
    }

    @Override
    protected RepositoryEventDispatcher createRepositoryEventDispatcher() {
        return memoize(RepositoryEventDispatcher.class, super.createRepositoryEventDispatcher());
    }

    @Override
    protected Map<String, TrustedChecksumsSource> createTrustedChecksumsSources() {
        return memoize(TrustedChecksumsSource.class, super.createTrustedChecksumsSources());
    }

    @Override
    protected Map<String, ProvidedChecksumsSource> createProvidedChecksumsSources() {
        return memoize(ProvidedChecksumsSource.class, super.createProvidedChecksumsSources());
    }

    @Override
    protected Map<String, ChecksumExtractorStrategy> createChecksumExtractorStrategies() {
        return memoize(ChecksumExtractorStrategy.class, super.createChecksumExtractorStrategies());
    }

    @Override
    protected ChecksumProcessor createChecksumProcessor() {
        return memoize(ChecksumProcessor.class, super.createChecksumProcessor());
    }

    @Override
    protected ChecksumExtractor createChecksumExtractor() {
        return memoize(ChecksumExtractor.class, super.createChecksumExtractor());
    }

    @Override
    protected Map<String, TransporterFactory> createTransporterFactories() {
        return memoize(TransporterFactory.class, super.createTransporterFactories());
    }

    @Override
    protected TransporterProvider createTransporterProvider() {
        return memoize(TransporterProvider.class, super.createTransporterProvider());
    }

    @Override
    protected BasicRepositoryConnectorFactory createBasicRepositoryConnectorFactory() {
        return memoize(BasicRepositoryConnectorFactory.class, super.createBasicRepositoryConnectorFactory());
    }

    @Override
    protected Map<String, RepositoryConnectorFactory> createRepositoryConnectorFactories() {
        return memoize(RepositoryConnectorFactory.class, super.createRepositoryConnectorFactories());
    }

    @Override
    protected RepositoryConnectorProvider createRepositoryConnectorProvider() {
        return memoize(RepositoryConnectorProvider.class, super.createRepositoryConnectorProvider());
    }

    @Override
    protected Installer createInstaller() {
        return memoize(Installer.class, super.createInstaller());
    }

    @Override
    protected Deployer createDeployer() {
        return memoize(Deployer.class, super.createDeployer());
    }

    @Override
    protected Map<String, DependencyCollectorDelegate> createDependencyCollectorDelegates() {
        return memoize(DependencyCollectorDelegate.class, super.createDependencyCollectorDelegates());
    }

    @Override
    protected DependencyCollector createDependencyCollector() {
        return memoize(DependencyCollector.class, super.createDependencyCollector());
    }

    @Override
    protected Map<String, ArtifactResolverPostProcessor> createArtifactResolverPostProcessors() {
        return memoize(ArtifactResolverPostProcessor.class, super.createArtifactResolverPostProcessors());
    }

    @Override
    protected ArtifactResolver createArtifactResolver() {
        return memoize(ArtifactResolver.class, super.createArtifactResolver());
    }

    @Override
    protected MetadataResolver createMetadataResolver() {
        return memoize(MetadataResolver.class, super.createMetadataResolver());
    }

    @Override
    protected Map<String, MetadataGeneratorFactory> createMetadataGeneratorFactories() {
        return memoize(MetadataGeneratorFactory.class, super.createMetadataGeneratorFactories());
    }

    @Override
    protected ArtifactDescriptorReader createArtifactDescriptorReader() {
        return memoize(ArtifactDescriptorReader.class, super.createArtifactDescriptorReader());
    }

    @Override
    protected VersionResolver createVersionResolver() {
        return memoize(VersionResolver.class, super.createVersionResolver());
    }

    @Override
    protected VersionRangeResolver createVersionRangeResolver() {
        return memoize(VersionRangeResolver.class, super.createVersionRangeResolver());
    }

    @Override
    protected ModelBuilder createModelBuilder() {
        return memoize(ModelBuilder.class, super.createModelBuilder());
    }

    @Override
    protected ArtifactPredicateFactory createArtifactPredicateFactory() {
        return memoize(ArtifactPredicateFactory.class, super.createArtifactPredicateFactory());
    }

    @Override
    protected VersionScheme createVersionScheme() {
        return memoize(VersionScheme.class, super.createVersionScheme());
    }

    @Override
    protected Map<String, ArtifactGeneratorFactory> createArtifactGeneratorFactories() {
        return memoize(ArtifactGeneratorFactory.class, super.createArtifactGeneratorFactories());
    }

    @Override
    protected Map<String, ArtifactDecoratorFactory> createArtifactDecoratorFactories() {
        return memoize(ArtifactDecoratorFactory.class, super.createArtifactDecoratorFactories());
    }

    @Override
    protected ModelCacheFactory createModelCacheFactory() {
        return memoize(ModelCacheFactory.class, super.createModelCacheFactory());
    }
}
