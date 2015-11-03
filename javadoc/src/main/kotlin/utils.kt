package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.path

val DocumentationNode.qualifiedName: String
    get() = this.path.filter { it.kind == DocumentationNode.Kind.Package || it.kind in DocumentationNode.Kind.classLike }
                .map { it.name }
                .joinToString(".")
