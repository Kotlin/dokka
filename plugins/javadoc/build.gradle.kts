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

}

configureBintrayPublication("javadocPlugin")

configurations {

}