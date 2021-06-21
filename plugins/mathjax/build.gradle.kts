import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    val jsoup_version: String by project
    testImplementation("org.jsoup:jsoup:$jsoup_version")
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":kotlin-analysis"))
}

registerDokkaArtifactPublication("mathjaxPlugin") {
    artifactId = "mathjax-plugin"
}
