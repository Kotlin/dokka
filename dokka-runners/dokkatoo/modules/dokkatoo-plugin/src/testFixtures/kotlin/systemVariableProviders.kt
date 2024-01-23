package org.jetbrains.dokka.dokkatoo.utils

import kotlin.properties.ReadOnlyProperty

// Utilities for fetching System Properties and Environment Variables via delegated properties


internal fun optionalSystemProperty() = optionalSystemProperty { it }

internal fun <T : Any> optionalSystemProperty(
  convert: (String) -> T?
): ReadOnlyProperty<Any, T?> =
  ReadOnlyProperty { _, property ->
    val value = System.getProperty(property.name)
    if (value != null) convert(value) else null
  }


internal fun systemProperty() = systemProperty { it }

internal fun <T> systemProperty(
  convert: (String) -> T
): ReadOnlyProperty<Any, T> =
  ReadOnlyProperty { _, property ->
    val value = requireNotNull(System.getProperty(property.name)) {
      "system property ${property.name} is unavailable"
    }
    convert(value)
  }


internal fun optionalEnvironmentVariable() = optionalEnvironmentVariable { it }

internal fun <T : Any> optionalEnvironmentVariable(
  convert: (String) -> T?
): ReadOnlyProperty<Any, T?> =
  ReadOnlyProperty { _, property ->
    val value = System.getenv(property.name)
    if (value != null) convert(value) else null
  }
