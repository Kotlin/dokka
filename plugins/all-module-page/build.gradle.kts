import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "all-modules-page-plugin"
}

dependencies {
    implementation(project(":plugins:base"))
}