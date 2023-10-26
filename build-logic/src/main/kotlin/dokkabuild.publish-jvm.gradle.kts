/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.MAVEN_JVM_PUBLICATION_NAME

plugins {
    id("dokkabuild.java")
    id("dokkabuild.publish-base")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing.publications.register<MavenPublication>(MAVEN_JVM_PUBLICATION_NAME) {
    from(components["java"])
}
