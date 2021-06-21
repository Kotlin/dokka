import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    compileOnly(project(":plugins:base"))
    implementation(project(":core:test-api"))
    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
    implementation(kotlin("test-junit"))
}

registerDokkaArtifactPublication("dokkaBaseTestUtils") {
    artifactId = "dokka-base-test-utils"
}
