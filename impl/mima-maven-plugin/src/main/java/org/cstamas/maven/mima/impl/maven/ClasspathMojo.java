package org.cstamas.maven.mima.impl.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cstamas.maven.mima.core.MimaResolver;
import org.cstamas.maven.mima.core.context.MimaContextOverrides;
import org.eclipse.aether.artifact.DefaultArtifact;

@Mojo(name = "classpath")
public class ClasspathMojo extends AbstractMimaMojo {
    @Parameter(required = true)
    private String artifact;

    @Parameter
    private boolean offline;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            DefaultArtifact a = new DefaultArtifact(artifact);
            MimaResolver resolver = getResolver(
                    MimaContextOverrides.Builder.create().offline(offline).build());
            String classpath = resolver.classpath(a);
            getLog().info("");
            getLog().info("Classpath of " + artifact + " artifact is:");
            getLog().info(classpath);
            getLog().info("");
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }
}
