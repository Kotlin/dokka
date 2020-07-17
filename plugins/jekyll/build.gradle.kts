import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
}

registerDokkaArtifactPublication("jekyllPlugin") {
    artifactId = "jekyll-plugin"
}
