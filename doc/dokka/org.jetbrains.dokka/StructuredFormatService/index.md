---
layout: api
title: StructuredFormatService
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [StructuredFormatService](index.html)


# StructuredFormatService



```
abstract public class StructuredFormatService
```


### Members


|[&lt;init&gt;](_init_.html)|**`public StructuredFormatService(locationService: LocationService, languageService: LanguageService)`**|
|[appendBlockCode](appendBlockCode.html)|**`abstract public fun appendBlockCode(to: StringBuilder, line: String): Unit`**<br/>**`abstract public fun appendBlockCode(to: StringBuilder, lines: Iterable<String>): Unit`**|
|[appendDescription](appendDescription.html)|**`open public fun appendDescription(to: StringBuilder, nodes: Iterable<DocumentationNode>): Unit`**|
|[appendHeader](appendHeader.html)|**`abstract public fun appendHeader(to: StringBuilder, text: String, level: Int): Unit`**|
|[appendLine](appendLine.html)|**`abstract public fun appendLine(to: StringBuilder, text: String): Unit`**<br/>**`abstract public fun appendLine(to: StringBuilder): Unit`**|
|[appendLocation](appendLocation.html)|**`open public fun appendLocation(to: StringBuilder, nodes: Iterable<DocumentationNode>): Unit`**|
|[appendOutline](appendOutline.html)|**`open public fun appendOutline(to: StringBuilder, nodes: Iterable<DocumentationNode>): Unit`**|
|[appendOutlineChildren](appendOutlineChildren.html)|**`abstract public fun appendOutlineChildren(to: StringBuilder, nodes: Iterable<DocumentationNode>): Unit`**|
|[appendOutlineHeader](appendOutlineHeader.html)|**`abstract public fun appendOutlineHeader(to: StringBuilder, node: DocumentationNode): Unit`**|
|[appendSummary](appendSummary.html)|**`open public fun appendSummary(to: StringBuilder, nodes: Iterable<DocumentationNode>): Unit`**|
|[appendTable](appendTable.html)|**`abstract public fun appendTable(to: StringBuilder, body: () -> Unit): Unit`**|
|[appendTableBody](appendTableBody.html)|**`abstract public fun appendTableBody(to: StringBuilder, body: () -> Unit): Unit`**|
|[appendTableCell](appendTableCell.html)|**`abstract public fun appendTableCell(to: StringBuilder, body: () -> Unit): Unit`**|
|[appendTableHeader](appendTableHeader.html)|**`abstract public fun appendTableHeader(to: StringBuilder, body: () -> Unit): Unit`**|
|[appendTableRow](appendTableRow.html)|**`abstract public fun appendTableRow(to: StringBuilder, body: () -> Unit): Unit`**|
|[appendText](appendText.html)|**`abstract public fun appendText(to: StringBuilder, text: String): Unit`**|
|[formatBold](formatBold.html)|**`abstract public fun formatBold(text: String): String`**|
|[formatBreadcrumbs](formatBreadcrumbs.html)|**`abstract public fun formatBreadcrumbs(items: Iterable<FormatLink>): String`**|
|[formatCode](formatCode.html)|**`abstract public fun formatCode(code: String): String`**|
|[formatLink](formatLink.html)|**`abstract public fun formatLink(text: String, location: Location): String`**<br/>**`open public fun formatLink(link: FormatLink): String`**|
|[formatText](formatText.html)|**`abstract public fun formatText(text: String): String`**|
|[link](link.html)|**`open public fun link(from: DocumentationNode, to: DocumentationNode): FormatLink`**<br/>**`open public fun link(from: DocumentationNode, to: DocumentationNode, extension: String): FormatLink`**|

