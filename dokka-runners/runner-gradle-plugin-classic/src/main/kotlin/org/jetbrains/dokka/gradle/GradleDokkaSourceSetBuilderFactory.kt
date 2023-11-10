/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectFactory

@Suppress("ObjectLiteralToLambda") // Will fail at runtime in Gradle versions <= 6.6
fun AbstractDokkaTask.gradleDokkaSourceSetBuilderFactory(): NamedDomainObjectFactory<GradleDokkaSourceSetBuilder> =
    NamedDomainObjectFactory { name -> GradleDokkaSourceSetBuilder(name, project, DokkaSourceSetIdFactory()) }

