File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
assert buildLog.text.contains( "[DEBUG] Runtimes.getRuntime: MavenRuntime" )
assert buildLog.text.contains( "junit:junit:jar:4.13.2" ) // minimal: error messages were improved/changed in Maven 3.9.x
assert buildLog.text.contains( "in offline mode" )
