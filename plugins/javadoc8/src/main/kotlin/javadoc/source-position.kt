package org.jetbrains.dokka.javadoc

import com.sun.javadoc.SourcePosition
import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.NodeKind
import java.io.File

class SourcePositionAdapter(val docNode: DocumentationNode) : SourcePosition {

    private val sourcePositionParts: List<String> by lazy {
        docNode.details(NodeKind.SourcePosition).firstOrNull()?.name?.split(":") ?: emptyList()
    }

    override fun file(): File? = if (sourcePositionParts.isEmpty()) null else File(sourcePositionParts[0])

    override fun line(): Int = sourcePositionParts.getOrNull(1)?.toInt() ?: -1

    override fun column(): Int = sourcePositionParts.getOrNull(2)?.toInt() ?: -1
}
