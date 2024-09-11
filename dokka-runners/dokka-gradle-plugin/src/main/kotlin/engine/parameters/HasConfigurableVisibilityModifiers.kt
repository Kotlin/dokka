/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

internal interface HasConfigurableVisibilityModifiers {

    @get:Input
    val documentedVisibilities: SetProperty<VisibilityModifier>

    /** Sets [documentedVisibilities] (overrides any previously set values). */
    fun documentedVisibilities(vararg visibilities: VisibilityModifier): Unit =
        documentedVisibilities.set(visibilities.asList())
}
