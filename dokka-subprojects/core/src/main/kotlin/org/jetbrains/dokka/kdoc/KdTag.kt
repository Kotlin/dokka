/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// @see - separate section
// @sample - separate section
// @author - separate section
// @since - separate section
// and other javadoc tags
//public val author: String?
//    public val since: String?
// @return
public interface KdTag : KdDocumented {
    public val name: String
}
