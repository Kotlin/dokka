/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.shared

import java.net.URL

public data class ExternalDocumentation(val documentationURL: URL, val packageList: PackageList)
