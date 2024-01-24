/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.templating

import org.jetbrains.dokka.links.DRI

public class ResolveLinkCommand(
    public val dri: DRI
): Command
