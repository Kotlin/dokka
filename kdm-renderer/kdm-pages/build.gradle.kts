/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// kdp start
plugins {
    kotlin("jvm")
    application
}

kotlin {
    // Pin the toolchain so compilation and execution agree no matter which JDK the IDE
    // or the Gradle launcher happens to use. 8 matches the repo baseline
    // (`org.jetbrains.dokka.javaToolchain.mainCompiler`) and runs under any newer JVM.
    jvmToolchain(8)
}

dependencies {
    implementation(project.dependencies.platform(libs.kotlinxSerialization.bom))
    implementation(libs.kotlinxSerialization.json)
}

application {
    mainClass.set("org.jetbrains.kotlin.documentation.pages.MainKt")
}
// kdp end
