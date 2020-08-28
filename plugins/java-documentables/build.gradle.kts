import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaJavaDocumentables") {
    artifactId = "dokka-java-documentables"
}

dependencies {
    implementation("org.jsoup:jsoup:1.12.1")
    api(project(":plugins:kotlin-analysis"))
}