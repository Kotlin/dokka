import org.jetbrains.configureBintrayPublication
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

publishing {
    publications {
        register<MavenPublication>("javadocPlugin") {
            artifactId = "javadoc-plugin"
            from(components["java"])
        }
    }
}

dependencies {
    implementation("com.soywiz.korlibs.korte:korte-jvm:1.10.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")
    implementation(project(":plugins:base"))
    implementation(project(":plugins:kotlin-as-java"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}

configureBintrayPublication("javadocPlugin")
