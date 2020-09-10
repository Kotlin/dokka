import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "dokka-all-modules-page"
}

dependencies {
    api(project(":plugins:location"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}