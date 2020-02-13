subprojects {
    apply {
        plugin("maven-publish")
    }

    dependencies {
        compileOnly(project(":core"))
        compileOnly(kotlin("stdlib-jdk8"))
//        compileOnly(project(":coreDependencies", configuration = "shadow")) // uncomment if IntelliJ does not recognize pacakges from IntelliJ

        testImplementation(project(":testApi"))
        testImplementation(kotlin("stdlib-jdk8"))
        testImplementation("junit:junit:4.13")
    }
}