import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "dokka-all-modules-page"
}

dependencies {
    implementation(project(":plugins:processing"))
    implementation(project(":plugins:parsers"))
    implementation(project(":plugins:location"))
}