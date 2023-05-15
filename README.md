# MIMA

MIMA goal is creating "MIni MAven", one-shop stop reusable Java 8 library, to use Maven Resolver wherever you are.

MIMA makes you be able to:
* effortlessly reuse Maven Resolver inside and outside of Maven
* if outside Maven, it prepares you the environment obeying Maven user configuration (if asked for)
* supports all the latest Resolver features

## What is this about?

Reusing Maven Resolver is hard, especially if you need same thing to work in a library that may be used
from within Maven (ie. as part of Mojo) but also from outside of Maven (like part of some CLI).

The purpose of MIMA is to address this issue, and make your library reusable wherever it runs. But let's step
back a bit.

Using Maven Resolver as a library was always hard: for start, resolver alone is "incomplete", in a sense that
code in https://github.com/apache/maven-resolver repository **does not contain any model classes to work
with**. They are provided by Maven itself, when resolver is integrated. Moreover, is incomplete in a sense
that **not all components are implemented**. Resolver in Maven is "completed" by resolver-provider Maven
module here https://github.com/apache/maven/tree/maven-3.9.x/maven-resolver-provider but even this alone module
is not enough: just check it's dependencies, there are models, builders. And finally, to be able to properly
augment user environment from outside of Maven, obey things as user `settings.xml` etc., you even need more.

While some half-solutions did exist so far (most popular was Resolver's `ServiceLocator`), these solutions were 
never complete: `ServiceLocator` never offered "full experience" as compared to Resolver in Sisu, it was missing
the dynamism Sisu offered (for example, it was not extensible). Moreover, `ServiceLocator` is **deprecated** in
latest Resolver releases, for a reason: it forces Resolver developers to apply some (archaic) compromises, while
developing Resolver components, forbids ctor injection, components must have default ctors and have to be 
written like "this or that", when managed by Sisu or `ServiceLocator`, components cannot be immutable, etc. 
`ServiceLocator` pretty much forces you to write components you had to write in "good old" Plexus days.

Moreover, recent changes in Maven 3.9.x explicitly **prevents creation of new `RepositorySystem` instances**, for 
a good reason: if you run within Maven, you have everything "offered on a plate" (just inject it): 
you have full environment initialized with user specs and setup. No need to reinvent the wheel: 
less code, less bugs. This made `ServiceLocator` defunct in Maven 3.9.x and beyond as well.

Finally, Maven project never offered one-stop shop solution to use Resolver as a library. There was `ServiceLocator`
(incomplete, deprecated, to be dropped), Guice module (incomplete, likely to be deprecated) and Sisu indexes (incomplete)
but nothing as "a whole", a glue that holds things together for most popular use of Resolver: make it usable as 
it "as in Maven but outside of Maven".

MIMA tries to go one step beyond: It's goal is to make Resolver easily reusable "as a library", outside of Maven
but inside of it as well. And to do that in transparent way.

## How to use

In short, compile and code against [context](context/) artifact (make it `compile` scope in project), this makes
Resolver API available as transitive dependency as well. Next, pick one or more non-conflicting [runtime](runtime/) 
artifact (make it `runtime` scope in project) that will provide implementation(s) at runtime.

"Non-conflicting runtime" means,  you should have only one `embedded-` and only one `standalone-` prefixed runtime 
artifact coexisting at classpath, at any time.

Furthermore, as Resolver uses [SLF4J](https://www.slf4j.org/) for logging, it is your responsibility is to provide a 
"backend" for logging facade, as MIMA does not pull in any by default.

Complete example of using MIMA in some project intended to be used standalone only:

```
  <dependencies>
  ...
    <!-- context: compile scope -->
    <dependency>
      <groupId>eu.maveniverse.maven.mima</groupId>
      <artifactId>context</artifactId>
      <version>${version.mima}</version>
    </dependency>
    <!-- runtime: runtime scope -->
    <dependency>
      <groupId>eu.maveniverse.maven.mima.runtime</groupId>
      <artifactId>standalone-static</artifactId>
      <version>${version.mima}</version>
      <scope>runtime</scope>
    </dependency>
    <!-- logging: runtime scope -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>${version.slf4j}</version>
      <scope>runtime</scope>
    </dependency>
  ...
  </dependencies>
```

To demonstrate, an example ["demo project"](demo/) is provided: it contains the "library" artifact, that contains
some business logic that needs Resolver (to calculate classpath for given artifact) and it works as "plain library"
as UTs demonstrate. But, to introduce a twist, this same library is then used as dependency in "library-maven-plugin",
a Maven Plugin that does the same thing by reusing the "library". Here also, Invoker ITs prove that the Mojo works
as expected. The `demo` subproject is being tested with Maven 3.9.x and Maven 3.8.x for correct behavior.

## Runtimes

MIMA offers several runtime options for several use cases.

### Embedded Maven

To be used when library using MIMA runs within Maven (for example within a Maven Plugin). This runtime has the 
highest priority.

```
    <dependency>
      <groupId>eu.maveniverse.maven.mima.runtime</groupId>
      <artifactId>embedded-maven</artifactId>
      <version>${version.mima}</version>
      <scope>runtime</scope>
    </dependency>
```

To use MIMA from within Maven, this is the only dependency needed. Logging backend is not needed either, it is 
provided by Maven itself. This runtime activates **only when runs within Maven**, remains dormant otherwise,
and has no transitive dependencies. In case of libraries intended to work in both modes, inside but outside of Maven 
as well, this dependency may be always present.

### Standalone Sisu

To be used when library using MIMA runs standalone. In this case you need to provide backend for SLF4J facade as well.

```
    <dependency>
      <groupId>eu.maveniverse.maven.mima.runtime</groupId>
      <artifactId>standalone-sisu</artifactId>
      <version>${version.mima}</version>
      <scope>runtime</scope>
    </dependency>
    <!-- logging: runtime scope -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>${version.slf4j}</version>
      <scope>runtime</scope>
    </dependency>
```

This runtime brings in all dependencies needed for MIMA standalone runtime, and best is to have it in `runtine` scope, 
is not needed during compile. The runtime uses [Eclipse Sisu](https://www.eclipse.org/sisu/) DI, same engine used 
by Maven itself.

This runtime may be used in case you already have an application that is using Sisu for DI as well, like apps 
using [Ollie](https://github.com/takari/ollie) or alike.

You are also required to provide SLF4J backend.

### Standalone Static

To be used when library using MIMA runs standalone, and presence of Eclipse Sisu DI (and Google Guice and transitive 
dependencies like Guava) is unwanted. This runtime has the lowest priority.

```
    <dependency>
      <groupId>eu.maveniverse.maven.mima.runtime</groupId>
      <artifactId>standalone-static</artifactId>
      <version>${version.mima}</version>
      <scope>runtime</scope>
    </dependency>
    <!-- logging: runtime scope -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>${version.slf4j}</version>
      <scope>runtime</scope>
    </dependency>
```

This runtime is similar to Resolver's deprecated `ServiceLocator`, as it does not use DI but "manually" wires 
things up. 

You are also required to provide SLF4J backend.

### Uber Standalone Runtimes

Both standalone runtimes provide `-uber` artifacts, that as name implies, are artifacts that contains all the needed
classes shaded (but not relocated) into one JAR and POM modified for dependencies (removed). These artifacts are 
added to simplify use of non-trivial transitive dependencies of Resolver.

**One big difference exists**: `-uber` artifacts of standalone runtimes does not include SLF4J API, it is 
you (integrator) who should provide **binary compatible SLF4J API (baseline is version 1.7.36) and SLF4J backend
at runtime**. Required binary compatible SLF4J artifacts at classpath are:

* SLF4J API [baseline](https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/1.7.36/)
* SLF4J jcl-over-slf4j [baseline](https://repo.maven.apache.org/maven2/org/slf4j/jcl-over-slf4j/1.7.36/)
* SLF4J backend of choice (that is compatible with used SLF4J API)

The `jcl-over-slf4j` is required for 
[Apache HttpClient 4.x](https://hc.apache.org/httpcomponents-client-4.5.x/index.html) logging purposes, as it uses
JCL logging.

## Things to be aware of

When your library executes within Maven, the context you get is actually coming from Maven:
* repositorySystem == provided by Maven
* repositorySystemSession == provided and preconfigured by Maven (w/ all user whistle and bells)
* remoteRepositories == the current project remote repositories

The session and remote repositories (and many other things) can be customized via overrides, but
the "with-user-settings" cannot.

When your library runs outside of Maven, the context you get is created "from the scratch", based
on initial overrides you provided:
* repositorySystem == provided by (already existing or booted) Sisu DI
* repositorySystemSession == provided by MIMA w/ or w/o user env (see overrides)
* remoteRepositories == "central" or those from overrides

In any case, context is properly set up to support all whistle and bells from latest resolver
(working outside of Maven, or from inside of Maven if 3.9.x or later is used with it) like
split repository, extensible checksum algorithms, modern transport (and obeys configuration like
timeouts or headers, same as Maven would), provided checksums, repository filtering, locking, etc.

MIMA supports all resolver configuration properties from https://maven.apache.org/resolver/configuration.html
when runs in standalone mode or embedded in Maven 3.9.x (that provides resolver that supports new features).

Note: overrides are "all or nothing", so for example IF there is a list of remote repositories, 
they REPLACE current repositories, if any.

## Requirements

Build time:
* Java LTS (currently 17.0.6+)
* Maven 3.9.x

Runtime:
* Java 8+
* Standalone: SLF4J backend (1.7.36 baseline) should be provided.
* Standalone Uber: SLF4J API and backend (1.7.36 baseline) should be provided, and for Apache HttpClient 4.x logging, ideally `jcl-over-slf4j` as well.
