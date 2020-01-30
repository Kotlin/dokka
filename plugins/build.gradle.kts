subprojects {
    apply {
        plugin("maven-publish")
    }

    dependencies {
        compileOnly(project(":core"))
        compileOnly(kotlin("stdlib-jdk8"))
        testImplementation(project(":testApi"))
    }
}