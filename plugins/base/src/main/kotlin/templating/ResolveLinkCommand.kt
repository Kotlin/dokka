package org.jetbrains.dokka.base.templating

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.jetbrains.dokka.links.DRI

class ResolveLinkCommand(val dri: DRI): Command
