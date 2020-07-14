import org.jetbrains.configurePublication

dependencies {
    implementation("com.soywiz.korlibs.korte:korte-jvm:1.10.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")
    implementation(project(":plugins:base"))
    implementation(project(":plugins:kotlin-as-java"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}

configurePublication("javadoc-plugin")
