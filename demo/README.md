# MIMA Demo

... currently also misused as some sort of IT, run as next step after building MIMA.

## What is this about?

The MIMA Demo showcases its usage outside and inside of Maven. 

It consists of 4 modules:

* `library` - the reusable "library" that uses MIMA to perform some Resolver task. In this example it 
resolves transitive dependencies of an artifact and simply returns a String containing
OS correct classpath for given artifact.
* `library-standalone-sisu` - mimics an "application" that reuses "library" in a "standalone" scenario
and uses Eclipse Sisu DI (hence, "standalone-sisu-uber" runtime is used). It merely invokes the
library and ensures "it works".
* `library-standalone-static` - mimics an "application" that reuses "library" in a "standalone" scenario
and does not uses Eclipse Sisu DI (hence, "standalone-static-uber" runtime is used). It merely invokes the
library and ensures "it works".
* `library-maven-plugin` - a Maven Plugin that reuses "library" within a Mojo running in Maven. To prove
it works, it has Maven Plugin IT implemented using maven-invoker-plugin, the usual way to 
implement Maven Plugin ITs.

Invoking Maven from `demo/` will run all these modules, and in case of success, the build will
finish successfully.
