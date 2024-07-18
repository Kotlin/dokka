package org.jetbrains.dokka.gradle.utils

import io.kotest.core.config.AbstractProjectConfig

@Suppress("unused") // this class is picked up by Kotest
class KotestProjectConfig : AbstractProjectConfig() {
    override var displayFullTestPath: Boolean? = true
}
