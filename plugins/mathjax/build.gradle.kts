import org.jetbrains.configureBintrayPublication

publishing {
    publications {
        register<MavenPublication>("mathjaxPlugin") {
            artifactId = "mathjax-plugin"
            from(components["java"])
        }
    }
}

configureBintrayPublication("mathjaxPlugin")
