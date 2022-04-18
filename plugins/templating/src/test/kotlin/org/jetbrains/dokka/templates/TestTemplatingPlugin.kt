package org.jetbrains.dokka.templates

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin

class TestTemplatingPlugin: DokkaPlugin() {

    val dokkaBase by lazy { plugin<DokkaBase>() }

    val allModulesPageGeneration by extending {
        (CoreExtensions.generation
                providing ::TestTemplatingGeneration
                override dokkaBase.singleGeneration)
    }
}