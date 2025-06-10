/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// tags:
// - ref + optional docs (@param, @throws)
// - docs (@return, @since, @author)
// - @see - TBD - multiple syntaxes
// - ref (@sample)

// KDoc tags (add JavaDoc tags)
// AUTHOR - PLAIN TAG
// SINCE - PLAIN TAG
// SEE - REFERENCE TAG (TBD)

// those tags will not map to `KdTag` at all

// RETURN - PLAIN TAG -> transforms into docs for return value
// THROWS/EXCEPTION - REFERENCE TAG -> transforms into docs for throw
// RECEIVER - PLAIN TAG -> transforms into docs for receiver
// PARAM - REFERENCE TAG -> transforms into docs for parameter
// CONSTRUCTOR - CONTEXT TAG -> transforms into docs for constructor
// PROPERTY - CONTEXT TAG -> transforms into docs for property
// SAMPLE - SPECIAL TAG -> TODO
// SUPPRESS - SPECIAL TAG -> skips declaration

// TODO: may be reference should also sometimes allow external links
// TODO: may be another name
@Serializable
public data class KdTag(
    public val name: String,
    public val reference: KdElementId? = null, // TODO: what type should be here?
    override val documentation: KdDocumentation? = null,
    // public val isCustom: Boolean?
) : KdDocumented()
