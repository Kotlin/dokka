import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("templating-plugin") {
    artifactId = "templating-plugin"
}

dependencies {
    implementation(project(":plugins:base"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    val jackson_version: String by project
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
    testImplementation(project(":plugins:base:base-test-utils"))
}