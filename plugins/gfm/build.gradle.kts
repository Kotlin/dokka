import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
}

registerDokkaArtifactPublication("gfmPlugin") {
    artifactId = "gfm-plugin"
}
