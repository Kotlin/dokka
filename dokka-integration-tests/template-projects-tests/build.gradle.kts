/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.File.pathSeparator

plugins {
    id("dokkabuild.kotlin-jvm")
//    id("dokkabuild.publish-base")
//    id("dokkabuild.gradle-plugin")
//    id("org.gradle.kotlin.kotlin-dsl")
//    kotlin("jvm")
    `test-suite-base`
    `java-test-fixtures`

}

dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
//    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20-SNAPSHOT")
//    compileOnly("org.jetbrains.dokka:dokka-core:latest")
//    compileOnly("org.jetbrains.dokka:dokka-gradle-plugin:latest")
//    compileOnly("org.jetbrains.dokka:dokka-gradle-plugin-classic:latest")

//    devPublication("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20-SNAPSHOT")
}

val gradlePluginClassic = gradle.includedBuild("runner-gradle-plugin-classic")
val dokkaSubprojects = gradle.includedBuild("dokka-subprojects")

val projectsUnderTest = listOf(
    gradlePluginClassic,
    dokkaSubprojects,
)

val testMavenDirs = files(projectsUnderTest.map { it.projectDir.resolve("build/maven-dev") })

tasks.integrationTestPreparation {
    dependsOn(
        projectsUnderTest.map {
//            it.task(":allProjectTasks_integrationTestPreparation")
            it.task(":updateDevRepo")
        }
    )
}

tasks.withType<Test>().configureEach {
    dependsOn(tasks.integrationTestPreparation)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()
            dependencies {
                implementation(projects.utilities)

                implementation(libs.kotlin.test)
                implementation(libs.junit.jupiterApi)
                implementation(libs.junit.jupiterParams)

                implementation(gradleTestKit())
            }

            targets.configureEach {
                testTask.configure {
                    testMavenDirs.forEach { testMavenDir -> inputs.dir(testMavenDir) }
                    systemProperty(
                        "projectLocalMavenDirs",
                        testMavenDirs.joinToString(pathSeparator) { it.invariantSeparatorsPath }
                    )

                    val templateProjectsDir = layout.projectDirectory.dir("templateProjects")
//                    inputs.dir(templateProjectsDir).withPropertyName("templateProjectsDir")
                    systemProperty("templateProjectsDir", templateProjectsDir.asFile.invariantSeparatorsPath)
                }
            }
        }

        register<JvmTestSuite>("basicProjectTest") {
            targets.configureEach {
                testTask.configure {
                    inputs.dir(layout.projectDirectory.dir("templateProjects/basicProject"))
                }
            }
        }

        register<JvmTestSuite>("basicProjectJdk8") {
            targets.register("jdk8")
            targets.configureEach {
                testTask.configure {
                    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
                }
            }
        }
    }

    tasks.check {
        dependsOn(suites)
    }
}


//region simplify src dirs
// get rid of the java src dirs, flatten the kotlin & source directories
//sourceSets {
//    configureEach {
//        val sourceName = name
//        kotlin { setSrcDirs(listOf("$sourceName/src")) }
//        resources { setSrcDirs(listOf("$sourceName/resources")) }
//        java { setSrcDirs(emptyList<File>()) }
//    }
//}
//@Suppress("UnstableApiUsage")
//testing {
//    suites.withType<JvmTestSuite>().configureEach {
//        val suiteName = name
//        sources {
//            kotlin { setSrcDirs(listOf("$suiteName/src")) }
//            resources { setSrcDirs(listOf("$suiteName/resources")) }
//            java { setSrcDirs(listOf(emptyList<File>())) }
//        }
//    }
//}
//endregion

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors = false
    }
}
