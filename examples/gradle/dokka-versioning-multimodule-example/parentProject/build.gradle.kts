import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

dependencies {
    implementation(kotlin("stdlib"))
}

val olderVersionsFolder = "olderVersions"

// The previously documentations should be generated with the versioning plugin
val generatePreviouslyDocTask by tasks.register<DokkaMultiModuleTask>("dokkaPreviouslyDocumentation") {
    dependencies {
        dokkaPlugin("org.jetbrains.dokka:all-modules-page-plugin:1.6.21")
        dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.6.21")
    }
    val configuredVersion = "0.9"
    outputDirectory.set(file(projectDir.toPath().resolve(olderVersionsFolder).resolve(configuredVersion)))
    pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.versioning.VersioningPlugin" to """{ "version": "$configuredVersion" }"""))
    addChildTasks(listOf(project("childProjectA"), project("childProjectB")), "dokkaHtmlPartial")
}

tasks.dokkaHtmlMultiModule {
    dependsOn(generatePreviouslyDocTask)
    val configuredVersion = "1.0"
    pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.versioning.VersioningPlugin" to """{ "version": "$configuredVersion", "olderVersionsDir": "$projectDir/$olderVersionsFolder" }"""))
}