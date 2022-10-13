import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
}

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
}

registerDokkaArtifactPublication("jekyllPlugin") {
    artifactId = "jekyll-plugin"
}
