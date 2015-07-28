package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.path
import java.util.*

val DocumentationNode.qualifiedName: String
    get() = this.path.filter { it.kind == DocumentationNode.Kind.Package || it.kind in allClassKinds }
                .map { it.name }
                .joinToString(".")
