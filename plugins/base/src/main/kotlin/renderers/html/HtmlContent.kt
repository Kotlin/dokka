package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.pages.ContentBreakLine
import org.jetbrains.dokka.pages.Style


/**
 * Html-specific style that represents <hr> tag if used in conjunction with [ContentBreakLine]
 */
internal object HorizontalBreakLineStyle : Style {
    // this exists as a simple internal solution to avoid introducing unnecessary public API on content level.
    // If you have the need to implement proper horizontal divider (i.e to support `---` markdown element),
    // consider removing this and providing proper API for all formats and levels
}
