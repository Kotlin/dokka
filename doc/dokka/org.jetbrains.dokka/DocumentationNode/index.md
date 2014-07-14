---
layout: post
title: DocumentationNode
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [DocumentationNode](index.md)

# DocumentationNode

```
open public class DocumentationNode
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|&nbsp;&nbsp;`public DocumentationNode(descriptor: DeclarationDescriptor, name: String, doc: DocumentationContent, kind: Kind)`<br>|
|[Kind](Kind/index.md)|&nbsp;&nbsp;`public enum class Kind`<br>|
|[addAllReferencesFrom](addAllReferencesFrom.md)|&nbsp;&nbsp;`public fun addAllReferencesFrom(other: DocumentationNode): Unit`<br>|
|[addReferenceTo](addReferenceTo.md)|&nbsp;&nbsp;`public fun addReferenceTo(to: DocumentationNode, kind: Kind): Unit`<br>|
|[allReferences](allReferences.md)|&nbsp;&nbsp;`public fun allReferences(): Set<DocumentationReference>`<br>|
|[descriptor](descriptor.md)|&nbsp;&nbsp;`val descriptor: DeclarationDescriptor`<br>|
|[detail](detail.md)|&nbsp;&nbsp;`public fun detail(kind: Kind): DocumentationNode`<br>|
|[details](details/index.md)|&nbsp;&nbsp;`public val details: List<DocumentationNode>`<br>|
|[details](details.md)|&nbsp;&nbsp;`public fun details(kind: Kind): List<DocumentationNode>`<br>|
|[doc](doc.md)|&nbsp;&nbsp;`val doc: DocumentationContent`<br>|
|[kind](kind.md)|&nbsp;&nbsp;`val kind: Kind`<br>|
|[link](link.md)|&nbsp;&nbsp;`public fun link(kind: Kind): DocumentationNode`<br>|
|[links](links/index.md)|&nbsp;&nbsp;`public val links: List<DocumentationNode>`<br>|
|[links](links.md)|&nbsp;&nbsp;`public fun links(kind: Kind): List<DocumentationNode>`<br>|
|[member](member.md)|&nbsp;&nbsp;`public fun member(kind: Kind): DocumentationNode`<br>|
|[members](members/index.md)|&nbsp;&nbsp;`public val members: List<DocumentationNode>`<br>|
|[members](members.md)|&nbsp;&nbsp;`public fun members(kind: Kind): List<DocumentationNode>`<br>|
|[name](name.md)|&nbsp;&nbsp;`val name: String`<br>|
|[owner](owner/index.md)|&nbsp;&nbsp;`public val owner: DocumentationNode`<br>|
|[references](references.md)|&nbsp;&nbsp;`private val references: LinkedHashSet<DocumentationReference>`<br>&nbsp;&nbsp;`public fun references(kind: Kind): List<DocumentationReference>`<br>|
|[toString](toString.md)|&nbsp;&nbsp;`open public fun toString(): String`<br>|
