/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import io.kotest.core.config.AbstractProjectConfig

@Suppress("unused") // this class is picked up by Kotest
class KotestProjectConfig : AbstractProjectConfig() {
    override var displayFullTestPath: Boolean? = true
}
