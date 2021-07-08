import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation("com.soywiz.korlibs.korte:korte-jvm:2.2.0")
    implementation(project(":plugins:base"))
    implementation(project(":plugins:kotlin-as-java"))
    testImplementation(project(":plugins:base:base-test-utils"))

    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}

registerDokkaArtifactPublication("javadocPlugin") {
    artifactId = "javadoc-plugin"
}
