/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.dokka.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.dokkatoo.dokka.parameters.VisibilityModifier.Companion.dokkaType

class VisibilityModifierTest : FunSpec({

    test("DokkaConfiguration.Visibility should have equivalent VisibilityModifier") {
        DokkaConfiguration.Visibility.values().shouldForAll { dokkaVisibility ->
            VisibilityModifier.entries.map { it.dokkaType }.shouldForOne { it shouldBe dokkaVisibility }
        }
    }
})
