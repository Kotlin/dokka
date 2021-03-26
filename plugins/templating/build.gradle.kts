import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("templating-plugin") {
    artifactId = "templating-plugin"
}

dependencies {
    implementation(project(":plugins:base"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

    implementation("org.jsoup:jsoup:1.13.1")
    testImplementation(project(":plugins:base:base-test-utils"))
}