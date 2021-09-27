dependencies {
    implementation(kotlin("stdlib"))
}

tasks.dokkaHtmlMultiModule {
    val configuredVersion = "1.0"
    // The previously documentations should be generated with the versioning plugin
    pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.versioning.VersioningPlugin" to """{ "version": "$configuredVersion", "olderVersionsDir": "$projectDir/olderVersions" }"""))
}