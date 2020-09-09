package org.jetbrains.dokka.location

import org.jetbrains.dokka.location.external.DefaultExternalLocationProviderFactory
import org.jetbrains.dokka.location.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.location.external.javadoc.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.location.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.location.local.LocationProviderFactory
import org.jetbrains.dokka.plugability.DokkaPlugin

class Location: DokkaPlugin() {
    val locationProviderFactory by extensionPoint<LocationProviderFactory>()
    val externalLocationProviderFactory by extensionPoint<ExternalLocationProviderFactory>()

    val locationProvider by extending {
        locationProviderFactory providing ::DokkaLocationProviderFactory
    }

    val javadocLocationProvider by extending {
        externalLocationProviderFactory providing ::JavadocExternalLocationProviderFactory
    }

    val dokkaLocationProvider by extending {
        externalLocationProviderFactory providing ::DefaultExternalLocationProviderFactory
    }
}

const val DOKKA_PARAM_PREFIX = "\$dokka"