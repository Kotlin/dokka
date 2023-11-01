/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.jetbrains.dokka.base.translators.documentables.DefaultPageCreator

internal inline fun withAllTypesPage(block: () -> Unit) {
    System.setProperty(DefaultPageCreator.SHOULD_DISPLAY_ALL_TYPES_PAGE_SYS_PROP, "true")
    try {
        block()
    } finally {
        System.clearProperty(DefaultPageCreator.SHOULD_DISPLAY_ALL_TYPES_PAGE_SYS_PROP)
    }
}
