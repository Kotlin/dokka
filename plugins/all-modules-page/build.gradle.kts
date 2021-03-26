import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "all-modules-page-plugin"
}

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:templating"))
    implementation(project(":plugins:versioning"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":plugins:gfm"))
    testImplementation(project(":plugins:gfm:gfm-template-processing"))
    testImplementation(project(":core:content-matcher-test-utils"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

    implementation("org.jsoup:jsoup:1.13.1")
}