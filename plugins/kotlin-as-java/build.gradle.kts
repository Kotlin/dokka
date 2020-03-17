import org.jetbrains.configureBintrayPublication

plugins {
    id("com.jfrog.bintray")
}

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