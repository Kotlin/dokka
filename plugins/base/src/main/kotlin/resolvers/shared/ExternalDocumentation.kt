package org.jetbrains.dokka.base.resolvers.shared

import java.net.URL

data class ExternalDocumentation(val documentationURL: URL, val packageList: PackageList)
