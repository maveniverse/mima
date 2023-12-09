package eu.maveniverse.maven.mima.runtime.standalonestatic;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;

/**
 *
 */
public class RepositorySystemSupplier extends org.eclipse.aether.supplier.RepositorySystemSupplier {
    @Override
    protected Map<String, TransporterFactory> getTransporterFactories() {
        HashMap<String, TransporterFactory> result = new HashMap<>();
        result.put(FileTransporterFactory.NAME, new FileTransporterFactory());
        result.put(JdkTransporterFactory.NAME, new JdkTransporterFactory());
        return result;
    }
}
