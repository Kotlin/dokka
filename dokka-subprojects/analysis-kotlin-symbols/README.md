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

## Testing with an override version of Analysis API

To build it with an override version of Analysis API, the property 
`org.jetbrains.dokka.build.overrideAnalysisAPIVersion=2.2.0-dev-*` should be added to the project. Any version can be set in the property instead of `2.2.0-dev-*`.

### Entry point

The main entry point is `DefaultSymbolToDocumentableTranslator` (this is an extension for [the extension point](https://kotlin.github.io/dokka/1.9.20/developer_guide/architecture/extension_points/core_extensions/#sourcetodocumentabletranslator) ), that is used by the Dokka core to build Documentable model by a source set.

Across running Dokka we keep `StandaloneAnalysisAPISession` and `KtSourceModule` instances from Analysis API into `KotlinAnalysis`.
`KotlinAnalysis` is used in `DefaultSymbolToDocumentableTranslator` and other services that need an additional analysis.

### How to run the Dokka tests with a custom version of Analysis API?

- To change the version of the Analysis API in Dokka, the property `kotlin-compiler-k2` in `gradle/libs.versions.toml`  should be changed to a needed version.
  _Note:_ You may need to add a local repository in `settings.gradle.kts`
  _Note #2:_ You can use the property `org.jetbrains.dokka.build.overrideAnalysisAPIVersion` instead of `gradle/libs.versions.toml`. 
- The `gradle  :symbolsTest` Gradle task should be used to run Dokka unit tests with the only K2 analysis
- The `gradle :descriptorsTest` task can be used to check the K1 analysis
- Dokka also has integration tests. To run them, `:integrationTest` with the property `org.jetbrains.dokka.integration_test.useK2=true`   (or `gradlew integrationTest -Porg.jetbrains.dokka.integration_test.useK2=true`) should be called.
  They are run on [TeamCity](https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_Dokka_DokkaIntegrationTestsK2?mode=builds#all-projects).
  _Note:_ Currently, the integration tests do not check generated content very well.  For testing analysis, It is better to run unit tests.

#### How to run the Dokka unit-tests with a custom version of Analysis API on TeamCity?

By default, we run unit tests on TeamCity against the latest Analysis API by a schedule [here](https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_Dokka_ScheduledDokkaTestsK2?mode=builds#all-projects).  It is possible to trigger a run with a custom version of Analysis API (the parameter `Analysis API version` should be used).



### How to build/publish Dokka locally?

- You can use the `gradle :publishToMavenLocal` task to publish Dokka locally.
- To build Dokka without running testing, using the `gradle assemble` task is recommended since the tests are time-consuming.

See the detailed guide [here](https://kotlin.github.io/dokka/1.9.20/developer_guide/workflow/).