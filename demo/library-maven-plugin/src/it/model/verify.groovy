/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
if (buildLog.text.contains("Unsupported Maven version")) {
    return // ignore the rest
}
assert buildLog.text.contains( "[DEBUG] Runtimes.getRuntime: MavenRuntime" )
assert buildLog.text.contains( "[INFO] Model of junit:junit:4.13.1 artifact is:" )
