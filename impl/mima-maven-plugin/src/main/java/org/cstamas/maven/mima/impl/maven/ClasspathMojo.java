package org.cstamas.maven.mima.impl.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cstamas.maven.mima.core.MimaResolver;
import org.eclipse.aether.artifact.DefaultArtifact;

@Mojo(name = "classpath")
public class ClasspathMojo extends AbstractMimaMojo {
    @Parameter(required = true)
    private String artifact;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            DefaultArtifact a = new DefaultArtifact(artifact);
            MimaResolver resolver = getResolver();
            getLog().info(resolver.classpath(a));
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }
}
