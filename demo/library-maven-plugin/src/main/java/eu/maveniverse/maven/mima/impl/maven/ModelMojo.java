/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mima.impl.maven;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.impl.library.Classpath;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.rtinfo.RuntimeInformation;

@Mojo(name = "model")
public class ModelMojo extends AbstractMojo {
    @Parameter(required = true)
    private String artifact;

    @Parameter
    private boolean offline;

    @Component
    private RuntimeInformation runtimeInformation;

    @Override
    public void execute() throws MojoExecutionException {
        if (runtimeInformation.getMavenVersion().startsWith("3.6")) {
            getLog().info("Unsupported Maven version: " + runtimeInformation.getMavenVersion());
            return;
        }
        try {
            Classpath classpath = new Classpath();
            ContextOverrides overrides =
                    ContextOverrides.create().offline(offline).build();

            String md = classpath.model(overrides, artifact);
            getLog().info("");
            getLog().info("Model of " + artifact + " artifact is:");
            getLog().info(md);
            getLog().info("");
        } catch (Exception e) {
            throw new MojoExecutionException("Error:", e);
        }
    }
}
