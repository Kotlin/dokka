/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.PublicationName

/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.java")
    id("dokkabuild.publish-base")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing.publications.register<MavenPublication>(PublicationName.JVM) {
    from(components["java"])
}
