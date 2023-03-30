package org.cstamas.maven.mima.impl.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cstamas.maven.mima.context.ContextOverrides;
import org.cstamas.maven.mima.impl.library.Classpath;

@Mojo(name = "classpath")
public class ClasspathMojo extends AbstractMojo {
    @Parameter(required = true)
    private String artifact;

    @Parameter
    private boolean offline;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Classpath classpath = new Classpath();
            ContextOverrides overrides =
                    ContextOverrides.Builder.create().offline(offline).build();

            String cp = classpath.classpath(overrides, artifact);
            getLog().info("");
            getLog().info("Classpath of " + artifact + " artifact is:");
            getLog().info(cp);
            getLog().info("");
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }
}
