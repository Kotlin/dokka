import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "all-modules-page-plugin"
}

dependencies {
    implementation(project(":plugins:base"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    implementation("org.jsoup:jsoup:1.12.1")
}