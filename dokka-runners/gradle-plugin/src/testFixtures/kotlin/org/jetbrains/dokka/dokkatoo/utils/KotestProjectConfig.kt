/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.utils

import io.kotest.core.config.AbstractProjectConfig

@Suppress("unused") // this class is automatically picked up by Kotest
object KotestProjectConfig : AbstractProjectConfig() {
    init {
        displayFullTestPath = true
    }
}
