/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.utilities.parseJson
import org.jetbrains.dokka.utilities.serializeAsCompactJson
import org.jetbrains.dokka.utilities.serializeAsPrettyJson

public fun DokkaConfigurationImpl(json: String): DokkaConfigurationImpl = parseJson(json)

public fun GlobalDokkaConfiguration(json: String): GlobalDokkaConfiguration = parseJson(json)

@Deprecated("Renamed to better distinguish between compact/pretty prints", ReplaceWith("this.toCompactJsonString()"))
public fun DokkaConfiguration.toJsonString(): String = this.toCompactJsonString()

@Deprecated("Renamed to better distinguish between compact/pretty prints", ReplaceWith("this.toCompactJsonString()"))
public fun <T : ConfigurableBlock> T.toJsonString(): String = this.toCompactJsonString()

/**
 * Serializes [DokkaConfiguration] as a machine-readable and compact JSON string.
 *
 * The returned string is not very human friendly as it will be difficult to parse by eyes due to it
 * being compact and in one line. If you want to show the output to a human being, see [toPrettyJsonString].
 */
public fun DokkaConfiguration.toCompactJsonString(): String = serializeAsCompactJson(this)

/**
 * Serializes [DokkaConfiguration] as a human-readable (pretty printed) JSON string.
 *
 * The returned string will have excessive line breaks and indents, which might not be
 * desirable when passing this value between API consumers/producers. If you want
 * a machine-readable and compact json string, see [toCompactJsonString].
 */
public fun DokkaConfiguration.toPrettyJsonString(): String = serializeAsPrettyJson(this)

/**
 * Serializes a [ConfigurableBlock] as a machine-readable and compact JSON string.
 *
 * The returned string is not very human friendly as it will be difficult to parse by eyes due to it
 * being compact and in one line. If you want to show the output to a human being, see [toPrettyJsonString].
 */
public fun <T : ConfigurableBlock> T.toCompactJsonString(): String = serializeAsCompactJson(this)

/**
 * Serializes a [ConfigurableBlock] as a human-readable (pretty printed) JSON string.
 *
 * The returned string will have excessive line breaks and indents, which might not be
 * desirable when passing this value between API consumers/producers. If you want
 * a machine-readable and compact json string, see [toCompactJsonString].
 */
public fun <T : ConfigurableBlock> T.toPrettyJsonString(): String = serializeAsCompactJson(this)
