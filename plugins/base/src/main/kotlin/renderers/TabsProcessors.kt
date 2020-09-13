package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.pages.ContentHeader
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentPage

interface Tab {
    val header: ContentHeader
    val body: ContentNode
}

//TODO naming sucks
interface TabsAdder {
    fun invoke(pageContext: ContentPage): List<Tab>
}