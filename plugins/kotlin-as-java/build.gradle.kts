import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))
    testImplementation("org.jsoup:jsoup:1.13.1")
}

registerDokkaArtifactPublication("kotlinAsJavaPlugin") {
    artifactId = "kotlin-as-java-plugin"
}
