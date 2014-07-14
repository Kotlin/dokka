---
layout: post
title: MarkdownFormatService
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [MarkdownFormatService](index.md)

# MarkdownFormatService

```
open public class MarkdownFormatService
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|&nbsp;&nbsp;`public MarkdownFormatService(locationService: LocationService, signatureGenerator: SignatureGenerator)`<br>|
|[extension](extension.md)|&nbsp;&nbsp;`open val extension: String`<br>|
|[format](format.md)|&nbsp;&nbsp;`open fun format(nodes: Iterable<DocumentationNode>, to: StringBuilder): Unit`<br>|
|[formatLocation](formatLocation.md)|&nbsp;&nbsp;`private fun StringBuilder.formatLocation(nodes: Iterable<DocumentationNode>): Unit`<br>|
|[formatSummary](formatSummary.md)|&nbsp;&nbsp;`private fun StringBuilder.formatSummary(nodes: Iterable<DocumentationNode>): Unit`<br>|
|[locationService](locationService.md)|&nbsp;&nbsp;`val locationService: LocationService`<br>|
|[signatureGenerator](signatureGenerator.md)|&nbsp;&nbsp;`val signatureGenerator: SignatureGenerator`<br>|
