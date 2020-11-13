import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
    implementation(project(":plugins:all-module-page"))
}

registerDokkaArtifactPublication("dokkaGfmAllModulePage") {
    artifactId = "gfm-all-module-page-plugin"
}