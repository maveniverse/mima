/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
assert buildLog.text.contains( "[DEBUG] Runtimes.getRuntime: MavenRuntime" )
assert buildLog.text.contains( "junit:junit:jar:4.13.2" ) // minimal: error messages were improved/changed in Maven 3.9.x
assert buildLog.text.contains( "in offline mode" )
