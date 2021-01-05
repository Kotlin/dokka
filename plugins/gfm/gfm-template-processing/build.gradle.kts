import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
    implementation(project(":plugins:all-modules-page"))
    implementation(project(":plugins:templating"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}

registerDokkaArtifactPublication("dokkaGfmTemplateProcessing") {
    artifactId = "gfm-template-processing-plugin"
}