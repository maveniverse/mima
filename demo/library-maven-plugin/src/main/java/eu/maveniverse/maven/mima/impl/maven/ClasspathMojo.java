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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
                    ContextOverrides.create().offline(offline).build();

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
