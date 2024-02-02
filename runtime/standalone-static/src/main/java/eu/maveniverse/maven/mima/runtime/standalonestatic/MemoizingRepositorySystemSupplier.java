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
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.http.ChecksumExtractor;

public class MemoizingRepositorySystemSupplier extends RepositorySystemSupplier implements Lookup {
    private final HashMap<Class<?>, Object> singulars = new HashMap<>();

    private final HashMap<Class<?>, Map<String, Object>> plurals = new HashMap<>();

    public MemoizingRepositorySystemSupplier() {
        memoize(RepositorySystem.class, super.get()); // to trigger filling up of memoized components
    }

    @Override
    public RepositorySystem get() {
        return lookup(RepositorySystem.class).orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    private <T> T memoize(Class<T> key, T instance) {
        singulars.put(key, instance);
        return instance;
    }

    private <T> Map<String, T> memoize(Class<T> key, Map<String, T> instances) {
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
    protected FileProcessor getFileProcessor() {
        return memoize(FileProcessor.class, super.getFileProcessor());
    }

    @Override
    protected TrackingFileManager getTrackingFileManager() {
        return memoize(TrackingFileManager.class, super.getTrackingFileManager());
    }

    @Override
    protected LocalPathComposer getLocalPathComposer() {
        return memoize(LocalPathComposer.class, super.getLocalPathComposer());
    }

    @Override
    protected LocalPathPrefixComposerFactory getLocalPathPrefixComposerFactory() {
        return memoize(LocalPathPrefixComposerFactory.class, super.getLocalPathPrefixComposerFactory());
    }

    @Override
    protected RepositorySystemLifecycle getRepositorySystemLifecycle() {
        return memoize(RepositorySystemLifecycle.class, super.getRepositorySystemLifecycle());
    }

    @Override
    protected OfflineController getOfflineController() {
        return memoize(OfflineController.class, super.getOfflineController());
    }

    @Override
    protected UpdatePolicyAnalyzer getUpdatePolicyAnalyzer() {
        return memoize(UpdatePolicyAnalyzer.class, super.getUpdatePolicyAnalyzer());
    }

    @Override
    protected ChecksumPolicyProvider getChecksumPolicyProvider() {
        return memoize(ChecksumPolicyProvider.class, super.getChecksumPolicyProvider());
    }

    @Override
    protected UpdateCheckManager getUpdateCheckManager(
            TrackingFileManager trackingFileManager, UpdatePolicyAnalyzer updatePolicyAnalyzer) {
        return memoize(
                UpdateCheckManager.class, super.getUpdateCheckManager(trackingFileManager, updatePolicyAnalyzer));
    }

    @Override
    protected Map<String, NamedLockFactory> getNamedLockFactories() {
        return memoize(NamedLockFactory.class, super.getNamedLockFactories());
    }

    @Override
    protected Map<String, NameMapper> getNameMappers() {
        return memoize(NameMapper.class, super.getNameMappers());
    }

    @Override
    protected NamedLockFactoryAdapterFactory getNamedLockFactoryAdapterFactory(
            Map<String, NamedLockFactory> namedLockFactories,
            Map<String, NameMapper> nameMappers,
            RepositorySystemLifecycle repositorySystemLifecycle) {
        return memoize(
                NamedLockFactoryAdapterFactory.class,
                super.getNamedLockFactoryAdapterFactory(namedLockFactories, nameMappers, repositorySystemLifecycle));
    }

    @Override
    protected SyncContextFactory getSyncContextFactory(NamedLockFactoryAdapterFactory namedLockFactoryAdapterFactory) {
        return memoize(SyncContextFactory.class, super.getSyncContextFactory(namedLockFactoryAdapterFactory));
    }

    @Override
    protected Map<String, ChecksumAlgorithmFactory> getChecksumAlgorithmFactories() {
        return memoize(ChecksumAlgorithmFactory.class, super.getChecksumAlgorithmFactories());
    }

    @Override
    protected ChecksumAlgorithmFactorySelector getChecksumAlgorithmFactorySelector(
            Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        return memoize(
                ChecksumAlgorithmFactorySelector.class,
                super.getChecksumAlgorithmFactorySelector(checksumAlgorithmFactories));
    }

    @Override
    protected Map<String, RepositoryLayoutFactory> getRepositoryLayoutFactories(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        return memoize(
                RepositoryLayoutFactory.class, super.getRepositoryLayoutFactories(checksumAlgorithmFactorySelector));
    }

    @Override
    protected RepositoryLayoutProvider getRepositoryLayoutProvider(
            Map<String, RepositoryLayoutFactory> repositoryLayoutFactories) {
        return memoize(RepositoryLayoutProvider.class, super.getRepositoryLayoutProvider(repositoryLayoutFactories));
    }

    @Override
    protected LocalRepositoryProvider getLocalRepositoryProvider(
            LocalPathComposer localPathComposer,
            TrackingFileManager trackingFileManager,
            LocalPathPrefixComposerFactory localPathPrefixComposerFactory) {
        return memoize(
                LocalRepositoryProvider.class,
                super.getLocalRepositoryProvider(
                        localPathComposer, trackingFileManager, localPathPrefixComposerFactory));
    }

    @Override
    protected RemoteRepositoryManager getRemoteRepositoryManager(
            UpdatePolicyAnalyzer updatePolicyAnalyzer, ChecksumPolicyProvider checksumPolicyProvider) {
        return memoize(
                RemoteRepositoryManager.class,
                super.getRemoteRepositoryManager(updatePolicyAnalyzer, checksumPolicyProvider));
    }

    @Override
    protected Map<String, RemoteRepositoryFilterSource> getRemoteRepositoryFilterSources(
            RepositorySystemLifecycle repositorySystemLifecycle, RepositoryLayoutProvider repositoryLayoutProvider) {
        return memoize(
                RemoteRepositoryFilterSource.class,
                super.getRemoteRepositoryFilterSources(repositorySystemLifecycle, repositoryLayoutProvider));
    }

    @Override
    protected RemoteRepositoryFilterManager getRemoteRepositoryFilterManager(
            Map<String, RemoteRepositoryFilterSource> remoteRepositoryFilterSources) {
        return memoize(
                RemoteRepositoryFilterManager.class,
                super.getRemoteRepositoryFilterManager(remoteRepositoryFilterSources));
    }

    @Override
    protected Map<String, RepositoryListener> getRepositoryListeners() {
        return memoize(RepositoryListener.class, super.getRepositoryListeners());
    }

    @Override
    protected RepositoryEventDispatcher getRepositoryEventDispatcher(
            Map<String, RepositoryListener> repositoryListeners) {
        return memoize(RepositoryEventDispatcher.class, super.getRepositoryEventDispatcher(repositoryListeners));
    }

    @Override
    protected Map<String, TrustedChecksumsSource> getTrustedChecksumsSources(
            FileProcessor fileProcessor,
            LocalPathComposer localPathComposer,
            RepositorySystemLifecycle repositorySystemLifecycle) {
        return memoize(
                TrustedChecksumsSource.class,
                super.getTrustedChecksumsSources(fileProcessor, localPathComposer, repositorySystemLifecycle));
    }

    @Override
    protected Map<String, ProvidedChecksumsSource> getProvidedChecksumsSources(
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        return memoize(ProvidedChecksumsSource.class, super.getProvidedChecksumsSources(trustedChecksumsSources));
    }

    @Override
    protected Map<String, ChecksumExtractor> getChecksumExtractors() {
        return memoize(ChecksumExtractor.class, super.getChecksumExtractors());
    }

    @Override
    protected Map<String, TransporterFactory> getTransporterFactories(Map<String, ChecksumExtractor> extractors) {
        return memoize(TransporterFactory.class, super.getTransporterFactories(extractors));
    }

    @Override
    protected TransporterProvider getTransporterProvider(Map<String, TransporterFactory> transporterFactories) {
        return memoize(TransporterProvider.class, super.getTransporterProvider(transporterFactories));
    }

    @Override
    protected BasicRepositoryConnectorFactory getBasicRepositoryConnectorFactory(
            TransporterProvider transporterProvider,
            RepositoryLayoutProvider repositoryLayoutProvider,
            ChecksumPolicyProvider checksumPolicyProvider,
            FileProcessor fileProcessor,
            Map<String, ProvidedChecksumsSource> providedChecksumsSources) {
        return memoize(
                BasicRepositoryConnectorFactory.class,
                super.getBasicRepositoryConnectorFactory(
                        transporterProvider,
                        repositoryLayoutProvider,
                        checksumPolicyProvider,
                        fileProcessor,
                        providedChecksumsSources));
    }

    @Override
    protected Map<String, RepositoryConnectorFactory> getRepositoryConnectorFactories(
            BasicRepositoryConnectorFactory basicRepositoryConnectorFactory) {
        return memoize(
                RepositoryConnectorFactory.class,
                super.getRepositoryConnectorFactories(basicRepositoryConnectorFactory));
    }

    @Override
    protected RepositoryConnectorProvider getRepositoryConnectorProvider(
            Map<String, RepositoryConnectorFactory> repositoryConnectorFactories,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        return memoize(
                RepositoryConnectorProvider.class,
                super.getRepositoryConnectorProvider(repositoryConnectorFactories, remoteRepositoryFilterManager));
    }

    @Override
    protected Installer getInstaller(
            FileProcessor fileProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            Map<String, MetadataGeneratorFactory> metadataGeneratorFactories,
            SyncContextFactory syncContextFactory) {
        return memoize(
                Installer.class,
                super.getInstaller(
                        fileProcessor, repositoryEventDispatcher, metadataGeneratorFactories, syncContextFactory));
    }

    @Override
    protected Deployer getDeployer(
            FileProcessor fileProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            UpdateCheckManager updateCheckManager,
            Map<String, MetadataGeneratorFactory> metadataGeneratorFactories,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController) {
        return memoize(
                Deployer.class,
                super.getDeployer(
                        fileProcessor,
                        repositoryEventDispatcher,
                        repositoryConnectorProvider,
                        remoteRepositoryManager,
                        updateCheckManager,
                        metadataGeneratorFactories,
                        syncContextFactory,
                        offlineController));
    }

    @Override
    protected Map<String, DependencyCollectorDelegate> getDependencyCollectorDelegates(
            RemoteRepositoryManager remoteRepositoryManager,
            ArtifactDescriptorReader artifactDescriptorReader,
            VersionRangeResolver versionRangeResolver) {
        return memoize(
                DependencyCollectorDelegate.class,
                super.getDependencyCollectorDelegates(
                        remoteRepositoryManager, artifactDescriptorReader, versionRangeResolver));
    }

    @Override
    protected DependencyCollector getDependencyCollector(
            Map<String, DependencyCollectorDelegate> dependencyCollectorDelegates) {
        return memoize(DependencyCollector.class, super.getDependencyCollector(dependencyCollectorDelegates));
    }

    @Override
    protected Map<String, ArtifactResolverPostProcessor> getArtifactResolverPostProcessors(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector,
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        return memoize(
                ArtifactResolverPostProcessor.class,
                super.getArtifactResolverPostProcessors(checksumAlgorithmFactorySelector, trustedChecksumsSources));
    }

    @Override
    protected ArtifactResolver getArtifactResolver(
            FileProcessor fileProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            VersionResolver versionResolver,
            UpdateCheckManager updateCheckManager,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController,
            Map<String, ArtifactResolverPostProcessor> artifactResolverPostProcessors,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        return memoize(
                ArtifactResolver.class,
                super.getArtifactResolver(
                        fileProcessor,
                        repositoryEventDispatcher,
                        versionResolver,
                        updateCheckManager,
                        repositoryConnectorProvider,
                        remoteRepositoryManager,
                        syncContextFactory,
                        offlineController,
                        artifactResolverPostProcessors,
                        remoteRepositoryFilterManager));
    }

    @Override
    protected MetadataResolver getMetadataResolver(
            RepositoryEventDispatcher repositoryEventDispatcher,
            UpdateCheckManager updateCheckManager,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        return memoize(
                MetadataResolver.class,
                super.getMetadataResolver(
                        repositoryEventDispatcher,
                        updateCheckManager,
                        repositoryConnectorProvider,
                        remoteRepositoryManager,
                        syncContextFactory,
                        offlineController,
                        remoteRepositoryFilterManager));
    }

    @Override
    protected Map<String, MetadataGeneratorFactory> getMetadataGeneratorFactories() {
        return memoize(MetadataGeneratorFactory.class, super.getMetadataGeneratorFactories());
    }

    @Override
    protected ArtifactDescriptorReader getArtifactDescriptorReader(
            RemoteRepositoryManager remoteRepositoryManager,
            VersionResolver versionResolver,
            VersionRangeResolver versionRangeResolver,
            ArtifactResolver artifactResolver,
            ModelBuilder modelBuilder,
            RepositoryEventDispatcher repositoryEventDispatcher,
            ModelCacheFactory modelCacheFactory) {
        return memoize(
                ArtifactDescriptorReader.class,
                super.getArtifactDescriptorReader(
                        remoteRepositoryManager,
                        versionResolver,
                        versionRangeResolver,
                        artifactResolver,
                        modelBuilder,
                        repositoryEventDispatcher,
                        modelCacheFactory));
    }

    @Override
    protected VersionResolver getVersionResolver(
            MetadataResolver metadataResolver,
            SyncContextFactory syncContextFactory,
            RepositoryEventDispatcher repositoryEventDispatcher) {
        return memoize(
                VersionResolver.class,
                super.getVersionResolver(metadataResolver, syncContextFactory, repositoryEventDispatcher));
    }

    @Override
    protected VersionRangeResolver getVersionRangeResolver(
            MetadataResolver metadataResolver,
            SyncContextFactory syncContextFactory,
            RepositoryEventDispatcher repositoryEventDispatcher) {
        return memoize(
                VersionRangeResolver.class,
                super.getVersionRangeResolver(metadataResolver, syncContextFactory, repositoryEventDispatcher));
    }

    @Override
    protected ModelBuilder getModelBuilder() {
        return memoize(ModelBuilder.class, super.getModelBuilder());
    }

    @Override
    protected ModelCacheFactory getModelCacheFactory() {
        return memoize(ModelCacheFactory.class, super.getModelCacheFactory());
    }
}
