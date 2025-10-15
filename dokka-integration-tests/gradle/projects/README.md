# Running integration tests projects locally

Projects used to run integration tests could be opened in or build independently by following these steps:

1. open `gradle.properties` file of a specific project and change commented variables:
    1. `dokka_it_kotlin_version` to required version of Kotlin Gradle Plugin, e.g 2.1.0
    2. `dokka_it_dokka_version` to required version to Dokka Gradle plugin, e.g 2.1.0
    3. replace all other commented properties if needed, like `dokka_it_android_gradle_plugin_version`
2. open `settings.gradle.kts` and replace `apply(from = "template.settings.gradle.kts")` with
   `apply(from = "../template.settings.gradle.kts")` (so the path should be prefixed with `../`)
3. (Optional) replace `/* %{DOKKA_IT_MAVEN_REPO}% */` with custom maven repositories like `mavenLocal` in
   `template.settings.gradle.kts` if dependencies should be resolved from places other than Maven Central.
4. Now it's possible to link a project in IDEA by right-clicking on `settings.gradle.kts` of a project and selecting
   `Link Gradle Project`

Note: for `ui-showcase` project those 1 and 2 steps should be done for both root of the project and `build-logic`.
