/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.plugins

import org.gradle.api.Named
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import java.io.Serializable
import javax.inject.Inject

/**
 * Base class for defining Dokka Plugin configuration.
 *
 * This class should not be instantiated directly. Instead, implement a subclass.
 *
 * @param[pluginFqn] Fully qualified classname of the Dokka Plugin
 */
abstract class DokkaPluginParametersBaseSpec
@DokkaInternalApi
@Inject
constructor(
    private val name: String,
    @get:Input
    open val pluginFqn: String,
) : Serializable, Named {

    abstract fun jsonEncode(): String // to be implemented by subclasses

    @Input
    override fun getName(): String = name
}
