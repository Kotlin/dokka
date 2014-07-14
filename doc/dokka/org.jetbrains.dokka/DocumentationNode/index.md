[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [DocumentationNode](index.md)

# DocumentationNode

```
open public class DocumentationNode
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|`public DocumentationNode(descriptor: DeclarationDescriptor, name: String, doc: DocumentationContent, kind: Kind)`<br>|
|[Kind](Kind/index.md)|`public enum class Kind`<br>|
|[addAllReferencesFrom](addAllReferencesFrom.md)|`public fun addAllReferencesFrom(other: DocumentationNode): Unit`<br>|
|[addReferenceTo](addReferenceTo.md)|`public fun addReferenceTo(to: DocumentationNode, kind: Kind): Unit`<br>|
|[allReferences](allReferences.md)|`public fun allReferences(): Set<DocumentationReference>`<br>|
|[descriptor](descriptor.md)|`val descriptor: DeclarationDescriptor`<br>|
|[detail](detail.md)|`public fun detail(kind: Kind): DocumentationNode`<br>|
|[details](details/index.md)|`public val details: List<DocumentationNode>`<br>|
|[details](details.md)|`public fun details(kind: Kind): List<DocumentationNode>`<br>|
|[doc](doc.md)|`val doc: DocumentationContent`<br>|
|[kind](kind.md)|`val kind: Kind`<br>|
|[link](link.md)|`public fun link(kind: Kind): DocumentationNode`<br>|
|[links](links/index.md)|`public val links: List<DocumentationNode>`<br>|
|[links](links.md)|`public fun links(kind: Kind): List<DocumentationNode>`<br>|
|[member](member.md)|`public fun member(kind: Kind): DocumentationNode`<br>|
|[members](members/index.md)|`public val members: List<DocumentationNode>`<br>|
|[members](members.md)|`public fun members(kind: Kind): List<DocumentationNode>`<br>|
|[name](name.md)|`val name: String`<br>|
|[owner](owner/index.md)|`public val owner: DocumentationNode`<br>|
|[references](references.md)|`private val references: LinkedHashSet<DocumentationReference>`<br><br>`public fun references(kind: Kind): List<DocumentationReference>`<br>|
|[toString](toString.md)|`open public fun toString(): String`<br>|
