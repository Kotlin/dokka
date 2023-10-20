/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import org.jetbrains.MAVEN_JVM_PUBLICATION_NAME

plugins {
    id("org.jetbrains.conventions.base-java")
    id("org.jetbrains.conventions.publishing-base")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing.publications.register<MavenPublication>(MAVEN_JVM_PUBLICATION_NAME) {
    from(components["java"])
}
