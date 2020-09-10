import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaHtml") {
    artifactId = "dokka-html"
}

dependencies {
    api(project(":plugins:rendering"))
    api(project(":plugins:processing"))

    val kotlinx_html_version: String by project
    api("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
}