# MIMA

This is an experiment of creating "MIni MAven" reusable library to use Maven Resolver wherever you are.

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
written like "this or that", when managed by Sisu or `ServiceLocator`, due ServiceLocator, components cannot be 
immutable, etc. ServiceLocator pretty much forces you to write components you had to write in "good old" Plexus days.

Moreover, recent changes in Maven 3.9.1 explicitly **prevents creation of new `RepositorySystem` instances**, for 
a good reason: if you run within Maven, you have everything "offered" (just inject it), no need to reinvent the
wheel, you have full environment initialized with user environment and setup. No need to reinvent the wheel: 
less code, less bugs. This made `ServiceLocator` defunct in Maven 3.9.1 as well.

Finally, Maven project never offered one-stop shop like solution to use Resolver as a library. There was `ServiceLocator`
(incomplete, deprecated, to be dropped), Guice module (incomplete, likely to be deprecated) and Sisu indexes (incomplete)
but nothing as "a whole", a glue that holds things together for most popular use of Resolver: use it "as in Maven but
outside of Maven".

MIMA tries to go one step beyond: It's goal is to make Resolver easily reusable "as a library", outside of Maven
but inside of it as well. And to do that in transparent way.