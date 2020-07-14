import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:test-utils"))
    testImplementation(project(":test-tools"))
}

registerDokkaArtifactPublication("kotlinAsJavaPlugin") {
    artifactId = "kotlin-as-java-plugin"
}
