/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains

enum class DokkaVersionType(val suffix: Regex) {
    RELEASE("^$".toRegex()),
    RC("RC\\d?".toRegex()),
    SNAPSHOT("SNAPSHOT".toRegex()),
    DEV("dev-\\d+".toRegex());
}
