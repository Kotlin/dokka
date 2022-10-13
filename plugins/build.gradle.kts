//import org.gradle.api.tasks.testing.logging.TestExceptionFormat
//import org.gradle.api.tasks.testing.logging.TestLogEvent
//
//plugins {
//    org.jetbrains.conventions.`kotlin-jvm`
//    org.jetbrains.conventions.`maven-publish`
//}
//
//subprojects {
//    apply {
//        plugin("maven-publish")
//    }
//
//    dependencies {
//        compileOnly(project(":core"))
//        implementation(kotlin("stdlib-jdk8"))
//        implementation(kotlin("stdlib"))
//        implementation(kotlin("reflect"))
//
//        testImplementation(project(":test-utils"))
//        testImplementation(project(":core:test-api"))
//        testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
//    }
//
//    tasks.test {
//        useJUnitPlatform()
//        testLogging {
//            exceptionFormat = TestExceptionFormat.FULL
//            events(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
//            showExceptions = true
//            showCauses = true
//            showStackTraces = true
//        }
//    }
//}
