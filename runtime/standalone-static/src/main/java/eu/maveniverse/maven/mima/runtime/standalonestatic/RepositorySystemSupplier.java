package eu.maveniverse.maven.mima.runtime.standalonestatic;

import java.util.Map;
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
}
