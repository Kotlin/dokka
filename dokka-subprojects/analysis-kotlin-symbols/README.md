# Analysis: Kotlin symbols

An internal symbols-based implementation for [analysis-kotlin-api](../analysis-kotlin-api). This implementation is 
also known as K2 or "the new compiler".

Contains no stable public API and must not be used by anyone directly, only via [analysis-kotlin-api](../analysis-kotlin-api).

Can be added as a runtime dependency by the runner.

## Shadowing

The `.jar` produced by this project shadows all dependencies. There are several reasons for it:

1. Some of the artifacts Dokka depends on, like `com.jetbrains.intellij.java:java-psi`, are not
   published to Maven Central, so the users would need to add custom repositories to their build scripts.
2. There are many intertwining transitive dependencies of different versions, as well as direct copy-paste,
   that can lead to runtime errors due to classpath conflicts, so it's best to let Gradle take care of
   dependency resolution, and then pack everything into a single jar in a single place that can be tuned.
