/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import org.gradle.jvm.tasks.Jar
import org.jetbrains.MAVEN_PUBLICATION_NAME

plugins {
    id("org.jetbrains.conventions.publishing-base")
}

val javadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    // TODO: decide if we really need dokka html in javadoc jar
//    description = "Assembles a Javadoc JAR using Dokka HTML"
//    from(tasks.dokkaHtml)
}

publishing.publications.register<MavenPublication>(MAVEN_PUBLICATION_NAME) {
    artifact(javadocJar)
    from(components["java"])
}
