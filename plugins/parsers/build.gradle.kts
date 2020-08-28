import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaParsers") {
    artifactId = "dokka-parsers"
}

dependencies {
    implementation(project(":plugins:kotlin-analysis"))
}