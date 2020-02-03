publishing {
    publications {
        register<MavenPublication>("mathjaxPlugin") {
            artifactId = "mathjax-plugin"
            from(components["java"])
        }
    }
}