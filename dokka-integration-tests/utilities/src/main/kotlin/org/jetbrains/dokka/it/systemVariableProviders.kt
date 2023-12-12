/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import kotlin.properties.ReadOnlyProperty

// Utilities for fetching System Properties and Environment Variables via delegated properties


internal fun <T : Any> optionalSystemProperty(
  convert: (String) -> T?
): ReadOnlyProperty<Any, T?> =
  ReadOnlyProperty { _, property ->
    val value = System.getProperty(property.name)
    if (value != null) convert(value) else null
  }


fun <T> systemProperty(
  convert: (String) -> T
): ReadOnlyProperty<Any, T> =
  ReadOnlyProperty { _, property ->
    val value = requireNotNull(System.getProperty(property.name)) {
      "system property ${property.name} is unavailable"
    }
    convert(value)
  }


internal fun <T : Any> optionalEnvironmentVariable(
  convert: (String) -> T?
): ReadOnlyProperty<Any, T?> =
  ReadOnlyProperty { _, property ->
    val value = System.getenv(property.name)
    if (value != null) convert(value) else null
  }
