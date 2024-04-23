 /*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.utils.declarable
import dokkabuild.utils.resolvable
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.SHADOWED
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE


val dokkaCli: Configuration by configurations.creating {
    description = "Dependency on Dokka CLI JAR. Must only contain a single dependency."
    declarable()
}

val dokkaCliResolver: Configuration by configurations.creating {
    description = "Resolve the Dokka CLI JAR. Intransitive - must only contain a single JAR."
    resolvable()
    extendsFrom(dokkaCli)
    attributes {
        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
        attribute(BUNDLING_ATTRIBUTE, objects.named(SHADOWED))
    }
    // we should have single artifact here
    isTransitive = false
}


val dokkaPluginsClasspath: Configuration by configurations.creating {
    description = "Dokka CLI runtime dependencies required to run Dokka CLI, and its plugins."
    declarable()
}

val dokkaPluginsClasspathResolver: Configuration by configurations.creating {
    description = "Resolve Dokka CLI runtime dependencies required to run Dokka CLI, and its plugins."
    resolvable()
    extendsFrom(dokkaPluginsClasspath)
    attributes {
        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
    }
}
