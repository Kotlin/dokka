package org.jetbrains.dokka.location.shared

import java.net.URL

data class ExternalDocumentation(val documentationURL: URL, val packageList: PackageList)
