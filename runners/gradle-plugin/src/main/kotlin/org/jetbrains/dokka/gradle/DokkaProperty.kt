/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.provider.Provider


internal fun Provider<String>.getValidVersionOrNull() = orNull?.takeIf { it != "unspecified" }
