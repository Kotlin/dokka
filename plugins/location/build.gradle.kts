import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaRendering") {
    artifactId = "dokka-rendering"
}

dependencies {
    api(project(":plugins:kotlin-analysis"))
}