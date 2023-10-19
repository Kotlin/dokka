/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

fun ProviderFactory.dokkaProperty(name: String): Provider<String> =
    gradleProperty("org.jetbrains.dokka.$name")
