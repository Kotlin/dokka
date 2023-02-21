package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaConfiguration

sealed class DokkaMultiModuleOrder {
    abstract val comparator: Comparator<DokkaConfiguration.DokkaModuleDescription>

    object Default : DokkaMultiModuleOrder() {
        override val comparator: Comparator<DokkaConfiguration.DokkaModuleDescription> =
            compareBy { it.relativePathToOutputDirectory }
    }

    object AsSpecified : DokkaMultiModuleOrder() {
        override val comparator: Comparator<DokkaConfiguration.DokkaModuleDescription> =
            compareBy { 0 }
    }

    class Custom(override val comparator: Comparator<DokkaConfiguration.DokkaModuleDescription>) : DokkaMultiModuleOrder()
}