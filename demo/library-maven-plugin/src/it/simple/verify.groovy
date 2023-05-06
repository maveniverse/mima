File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
assert buildLog.text.contains( "[DEBUG] Runtimes.getRuntime: MavenRuntime" )
assert buildLog.text.contains( "[INFO] Classpath of junit:junit:4.13.1 artifact is:" )
