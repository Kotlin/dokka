package org.jetbrains.dokka.usages

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.usages.renderers.UsagesAdder

class UsagesPlugin : DokkaPlugin() {
    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }

    val usagesAdder by extending {
        dokkaBasePlugin.tabsAdder with UsagesAdder()
    }
}