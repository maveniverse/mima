/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.runtime.standalonestatic;

import eu.maveniverse.maven.mima.context.Lookup;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.internal.impl.collect.DependencyCollectorDelegate;
import org.eclipse.aether.internal.impl.synccontext.named.NameMapper;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.spi.artifact.decorator.ArtifactDecoratorFactory;
import org.eclipse.aether.spi.artifact.generator.ArtifactGeneratorFactory;
import org.eclipse.aether.spi.artifact.transformer.ArtifactTransformer;
import org.eclipse.aether.spi.checksums.ProvidedChecksumsSource;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.PipelineRepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractorStrategy;
import org.eclipse.aether.spi.locking.LockingInhibitorFactory;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.eclipse.aether.spi.validator.ValidatorFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;

public class MemoizingRepositorySystemSupplierLookup implements Lookup {
    private final MimaRepositorySystemSupplier repositorySystemSupplier;

    public MemoizingRepositorySystemSupplierLookup() {
        this(Collections.emptyMap());
    }

    public MemoizingRepositorySystemSupplierLookup(Map<Class<?>, Map<String, Object>> staticExtensions) {
        this.repositorySystemSupplier = new MimaRepositorySystemSupplier(staticExtensions);
    }

    public RepositorySystem get() {
        return lookup(RepositorySystem.class).orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        return lookup(type, "default");
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        return Optional.ofNullable(lookupMap(false, type).get(name));
    }

    @SuppressWarnings({"unchecked"})
    private <T> Map<String, T> lookupMap(boolean tryPlural, Class<T> type) {
        // Factory -> Factories
        // Strategy -> Strategies
        // Mapper -> Mappers
        // Source -> Sources
        // Listener -> Listeners
        // Delegate -> Delegates
        // Processor -> Processors
        // Transformer -> Transformers
        String methodName = "get" + type.getSimpleName();
        if (tryPlural) {
            if (methodName.endsWith("y")) {
                methodName = methodName.substring(0, methodName.length() - 1) + "ies";
            } else {
                methodName = methodName + "s";
            }
        }
        try {
            Method method = MimaRepositorySystemSupplier.class.getMethod(methodName);
            Object result = method.invoke(repositorySystemSupplier);
            if (result instanceof Map) {
                return (Map<String, T>) result;
            }
            return Collections.singletonMap("default", (T) result);
        } catch (NoSuchMethodException e) {
            if (!tryPlural) {
                return lookupMap(true, type);
            } else {
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class MimaRepositorySystemSupplier extends RepositorySystemSupplier {
        private final Map<Class<?>, Map<String, Object>> staticExtensions;

        private MimaRepositorySystemSupplier(Map<Class<?>, Map<String, Object>> staticExtensions) {
            this.staticExtensions = staticExtensions;

            // validate: key class should be assignable from the value map values
            // (as they should be implementing key class, that is usually interface)
            for (Class<?> key : staticExtensions.keySet()) {
                Map<String, Object> values = staticExtensions.get(key);
                for (Object value : values.values()) {
                    if (!key.isInstance(value)) {
                        throw new IllegalArgumentException(String.format(
                                "User provided static extensions for key %s are of wrong type", key.getName()));
                    }
                }
            }
        }

        private static boolean isPresent(String clazzName) {
            try {
                MemoizingRepositorySystemSupplierLookup.class.getClassLoader().loadClass(clazzName);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        /**
         * This is special, it must work in url (without apache) case as well.
         */
        @Override
        protected Map<String, TransporterFactory> createTransporterFactories() {
            HashMap<String, TransporterFactory> result = new HashMap<>();
            result.put(FileTransporterFactory.NAME, new FileTransporterFactory());
            if (isPresent("org.eclipse.aether.transport.url.UrlTransporterFactory")) {
                result.put(
                        org.eclipse.aether.transport.url.UrlTransporterFactory.NAME,
                        new org.eclipse.aether.transport.url.UrlTransporterFactory(
                                getChecksumExtractor(), getPathProcessor()));
            }
            if (isPresent("org.eclipse.aether.transport.apache.ApacheTransporterFactory")) {
                result.put(
                        org.eclipse.aether.transport.apache.ApacheTransporterFactory.NAME,
                        new org.eclipse.aether.transport.apache.ApacheTransporterFactory(
                                getChecksumExtractor(), getPathProcessor()));
            }
            return withUserProvided(TransporterFactory.class, result);
        }

        private <T> Map<String, T> withUserProvided(Class<T> type, Map<String, T> base) {
            Map<String, T> result = new HashMap<>(base);
            Map<String, Object> userProvided = staticExtensions.get(type);
            if (userProvided != null) {
                userProvided.forEach((k, v) -> result.put(k, type.cast(v)));
            }
            return result;
        }

        @Override
        protected Map<String, NamedLockFactory> createNamedLockFactories() {
            return withUserProvided(NamedLockFactory.class, super.createNamedLockFactories());
        }

        @Override
        protected Map<String, NameMapper> createNameMappers() {
            return withUserProvided(NameMapper.class, super.createNameMappers());
        }

        @Override
        protected Map<String, LockingInhibitorFactory> createLockingInhibitorFactories() {
            return withUserProvided(LockingInhibitorFactory.class, super.createLockingInhibitorFactories());
        }

        @Override
        protected Map<String, ChecksumAlgorithmFactory> createChecksumAlgorithmFactories() {
            return withUserProvided(ChecksumAlgorithmFactory.class, super.createChecksumAlgorithmFactories());
        }

        @Override
        protected Map<String, RepositoryLayoutFactory> createRepositoryLayoutFactories() {
            return withUserProvided(RepositoryLayoutFactory.class, super.createRepositoryLayoutFactories());
        }

        @Override
        protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
            return withUserProvided(RemoteRepositoryFilterSource.class, super.createRemoteRepositoryFilterSources());
        }

        @Override
        protected Map<String, RepositoryListener> createRepositoryListeners() {
            return withUserProvided(RepositoryListener.class, super.createRepositoryListeners());
        }

        @Override
        protected Map<String, TrustedChecksumsSource> createTrustedChecksumsSources() {
            return withUserProvided(TrustedChecksumsSource.class, super.createTrustedChecksumsSources());
        }

        @Override
        protected Map<String, ProvidedChecksumsSource> createProvidedChecksumsSources() {
            return withUserProvided(ProvidedChecksumsSource.class, super.createProvidedChecksumsSources());
        }

        @Override
        protected Map<String, ChecksumExtractorStrategy> createChecksumExtractorStrategies() {
            return withUserProvided(ChecksumExtractorStrategy.class, super.createChecksumExtractorStrategies());
        }

        @Override
        protected Map<String, RepositoryConnectorFactory> createRepositoryConnectorFactories() {
            return withUserProvided(RepositoryConnectorFactory.class, super.createRepositoryConnectorFactories());
        }

        @Override
        protected Map<String, PipelineRepositoryConnectorFactory> createPipelineRepositoryConnectorFactories() {
            return withUserProvided(
                    PipelineRepositoryConnectorFactory.class, super.createPipelineRepositoryConnectorFactories());
        }

        @Override
        protected Map<String, DependencyCollectorDelegate> createDependencyCollectorDelegates() {
            return withUserProvided(DependencyCollectorDelegate.class, super.createDependencyCollectorDelegates());
        }

        @Override
        protected Map<String, ArtifactResolverPostProcessor> createArtifactResolverPostProcessors() {
            return withUserProvided(ArtifactResolverPostProcessor.class, super.createArtifactResolverPostProcessors());
        }

        @Override
        protected Map<String, ArtifactGeneratorFactory> createArtifactGeneratorFactories() {
            return withUserProvided(ArtifactGeneratorFactory.class, super.createArtifactGeneratorFactories());
        }

        @Override
        protected Map<String, ArtifactDecoratorFactory> createArtifactDecoratorFactories() {
            return withUserProvided(ArtifactDecoratorFactory.class, super.createArtifactDecoratorFactories());
        }

        @Override
        protected Map<String, ArtifactTransformer> createArtifactTransformers() {
            return withUserProvided(ArtifactTransformer.class, super.createArtifactTransformers());
        }

        @Override
        protected Map<String, MetadataGeneratorFactory> createMetadataGeneratorFactories() {
            return withUserProvided(MetadataGeneratorFactory.class, super.createMetadataGeneratorFactories());
        }

        @Override
        protected Map<String, ValidatorFactory> createValidatorFactories() {
            return withUserProvided(ValidatorFactory.class, super.createValidatorFactories());
        }
    }
}
