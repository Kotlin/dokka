import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    compileOnly(project(":plugins:base"))
    implementation(project(":core:test-api"))
    implementation("org.jsoup:jsoup:1.13.1")
    implementation(kotlin("test-junit"))
}

registerDokkaArtifactPublication("dokkaBaseTestUtils") {
    artifactId = "dokka-base-test-utils"
}
