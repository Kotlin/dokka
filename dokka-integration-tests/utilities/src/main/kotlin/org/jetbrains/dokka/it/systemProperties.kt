/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import kotlin.properties.ReadOnlyProperty

/**
 * Delegated accessor for a system property.
 *
 * @see System.getProperty
 */
fun systemProperty(): ReadOnlyProperty<Any?, String> =
    systemProperty { it }

/**
 * Delegated accessor for a system property.
 *
 * @see System.getProperty
 */
fun <T> systemProperty(
    convert: (String) -> T
): ReadOnlyProperty<Any?, T> =
    ReadOnlyProperty { _, property ->
        val value = requireNotNull(System.getProperty(property.name)) {
            "system property ${property.name} is unavailable"
        }
        convert(value)
    }

/**
 * Delegated accessor for a system property.
 *
 * @see System.getProperty
 */
fun optionalSystemProperty(): ReadOnlyProperty<Any?, String?> =
    optionalSystemProperty { it }

/**
 * Delegated accessor for a system property.
 *
 * @see System.getProperty
 */
fun <T> optionalSystemProperty(
    convert: (value: String?) -> T
): ReadOnlyProperty<Any?, T> =
    ReadOnlyProperty { _, property ->
        val value: String? = System.getProperty(property.name)
        convert(value)
    }
