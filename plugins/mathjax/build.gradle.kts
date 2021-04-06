import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    testImplementation("org.jsoup:jsoup:1.13.1")
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":kotlin-analysis"))
}

registerDokkaArtifactPublication("mathjaxPlugin") {
    artifactId = "mathjax-plugin"
}
