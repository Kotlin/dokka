import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:jekyll"))
    implementation(project(":plugins:all-modules-page"))
    implementation(project(":plugins:templating"))
    implementation(project(":plugins:gfm"))
    implementation(project(":plugins:gfm:gfm-template-processing"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}

registerDokkaArtifactPublication("dokkaJekyllTemplateProcessing") {
    artifactId = "jekyll-template-processing-plugin"
}