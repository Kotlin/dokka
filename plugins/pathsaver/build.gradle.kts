import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":kotlin-analysis"))
}

registerDokkaArtifactPublication("pathsaverPlugin") {
    artifactId = "pathsaver-plugin"
}
