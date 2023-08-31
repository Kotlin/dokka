/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.mock.MockApplication
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
public interface MockApplicationHack { // ¯\_(ツ)_/¯
    public fun hack(mockApplication: MockApplication)
}
