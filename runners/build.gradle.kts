subprojects {
    apply {
        plugin("maven-publish")
        plugin("com.jfrog.bintray")
    }

    tasks.processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
