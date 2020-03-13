import org.jetbrains.configureBintrayPublication

publishing {
    publications {
        register<MavenPublication>("kotlinAsJavaPlugin") {
            artifactId = "kotlin-as-java-plugin"
            from(components["java"])
        }
    }
}

dependencies {
    implementation(project(":plugins:base"))
}

configureBintrayPublication("kotlinAsJavaPlugin")