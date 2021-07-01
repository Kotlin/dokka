package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.model.WithChildren

data class NavigationNode(
    val id: String,
    val name: String,
    val location: String?,
    override val children: List<NavigationNode>
) : WithChildren<NavigationNode>
