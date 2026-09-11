/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: overall, `element` is something "linkable".
//  It makes sense to link to topics and section in it, even from outside of the current library;
//  e.g. to say something like: `check [coroutines:topic-name] about concurrency.
//  but it's not an "API", so it could be easily be broken
//  so, maybe, topic links should be like "internal"
//  for samples the story is not the same, as we don't really need to link them apart from the `@sample` tag.

// TODO: should it be an `element`?
// TODO: how java snippets should be represented
// TODO: `documentation` in sample feels strange?
// TODO: should it be just inside of a tag?
//  having it as an element, will allow samples to be a first-party thing, and so anyone could just easily find all samples for a module
//  overall, samples and topics (IMO) are very similar and interleaving a lot, so it's hard to say...
// TODO: what about my prototype on "resolve" inside of the samples?
@SerialName("sample")
@Serializable
public data class KdSample(
    val id: KdElementId,
    val name: String,
    public val code: String,
    public val language: String = "kotlin",
    override val documentation: List<KdDocumentationNode> = emptyList()
) : KdDocumented

// or article
@SerialName("topic")
@Serializable
public data class KdTopic(
    val id: KdElementId,
    val name: String,
    override val documentation: List<KdDocumentationNode> = emptyList()
) : KdDocumented
