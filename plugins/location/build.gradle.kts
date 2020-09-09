import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "dokka-all-modules-page"
}

dependencies {
    api(project(":plugins:kotlin-analysis"))
}