---
layout: api
title: DocumentationNode
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [DocumentationNode](index.html)


# DocumentationNode


```
open public class DocumentationNode
```

# Members

| Name | Summary |
|------|---------|
|[<init>](_init_.html)|**`public DocumentationNode(descriptor: DeclarationDescriptor, name: String, doc: DocumentationContent, kind: Kind)`**|
|[Kind](Kind/index.html)|**`public enum class Kind`**|
|[addAllReferencesFrom](addAllReferencesFrom.html)|**`public fun addAllReferencesFrom(other: DocumentationNode): Unit`**|
|[addReferenceTo](addReferenceTo.html)|**`public fun addReferenceTo(to: DocumentationNode, kind: Kind): Unit`**|
|[allReferences](allReferences.html)|**`public fun allReferences(): Set<DocumentationReference>`**|
|[detail](detail.html)|**`public fun detail(kind: Kind): DocumentationNode`**|
|[details](details/index.html)|**`public val details: List<DocumentationNode>`**|
|[details](details.html)|**`public fun details(kind: Kind): List<DocumentationNode>`**|
|[link](link.html)|**`public fun link(kind: Kind): DocumentationNode`**|
|[links](links/index.html)|**`public val links: List<DocumentationNode>`**|
|[links](links.html)|**`public fun links(kind: Kind): List<DocumentationNode>`**|
|[member](member.html)|**`public fun member(kind: Kind): DocumentationNode`**|
|[members](members/index.html)|**`public val members: List<DocumentationNode>`**|
|[members](members.html)|**`public fun members(kind: Kind): List<DocumentationNode>`**|
|[owner](owner/index.html)|**`public val owner: DocumentationNode`**|
|[references](references.html)|**`public fun references(kind: Kind): List<DocumentationReference>`**|
|[toString](toString.html)|**`open public fun toString(): String`**|
