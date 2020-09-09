package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.location.Location
import org.jetbrains.dokka.location.LocationProvider
import org.jetbrains.dokka.plugability.DokkaPlugin

class AllModulesPagePlugin: DokkaPlugin() {
    val allModulePageCreators by extending {
        CoreExtensions.allModulesPageCreator providing {
            MultimodulePageCreator(it)
        }
    }

    val multimoduleLocationProvider by extending {
        (plugin<Location>().locationProviderFactory
                providing  MultimoduleLocationProvider::Factory
                override plugin<Location>().locationProvider
                applyIf { modules.size > 1 })
    }
}