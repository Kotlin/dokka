package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.mock.MockApplication
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
interface MockApplicationHack { // ¯\_(ツ)_/¯
    fun hack(mockApplication: MockApplication)
}
