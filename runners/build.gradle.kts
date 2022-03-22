subprojects {
    apply {
        plugin("maven-publish")
    }

    tasks.processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
