/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.InternalDokkaApi

/**
 * Some parts of Dokka, specifically around [CustomDocTag], depend on this "MARKDOWN_FILE" constant.
 * However, it's unclear why exactly, and the chances of breaking something if it gets removed are not
 * zero, so it will stay as internal API for now, likely until the core data model is stabilized (#3365).
 *
 * You can depend on it in your existing code, but please refrain from using it in the new code
 * of your plugin unless absolutely necessary. If something does not work without using this constant,
 * please report your use case to https://github.com/Kotlin/dokka/issues/3365, it will help a lot.
 *
 * This constant is not marked with [InternalDokkaApi] on purpose. Even though it is discouraged to use it,
 * we understand that some existing code might depend on it, so once a replacement is provided,
 * this constant should be deprecated with a message that will help users migrate to something stable.
 */
public const val MARKDOWN_ELEMENT_FILE_NAME: String = "MARKDOWN_FILE"
