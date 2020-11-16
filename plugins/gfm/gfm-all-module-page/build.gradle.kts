import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
    implementation(project(":plugins:all-module-page"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}

registerDokkaArtifactPublication("dokkaGfmAllModulePage") {
    artifactId = "gfm-all-module-page-plugin"
}