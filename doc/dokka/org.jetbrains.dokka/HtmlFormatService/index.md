---
layout: api
title: HtmlFormatService
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [HtmlFormatService](index.html)


# HtmlFormatService


```
open public class HtmlFormatService
```

# Members

| Name | Summary |
|------|---------|
|[<init>](_init_.html)|**`public HtmlFormatService(locationService: LocationService, signatureGenerator: LanguageService)`**|
|[appendBlockCode](appendBlockCode.html)|**`open public fun appendBlockCode(to: StringBuilder, line: String): Unit`**<br/>**`open public fun appendBlockCode(to: StringBuilder, lines: Iterable<String>): Unit`**|
|[appendHeader](appendHeader.html)|**`open public fun appendHeader(to: StringBuilder, text: String, level: Int): Unit`**|
|[appendLine](appendLine.html)|**`open public fun appendLine(to: StringBuilder, text: String): Unit`**<br/>**`open public fun appendLine(to: StringBuilder): Unit`**|
|[appendOutlineChildren](appendOutlineChildren.html)|**`open public fun appendOutlineChildren(to: StringBuilder, nodes: Iterable<DocumentationNode>): Unit`**|
|[appendOutlineHeader](appendOutlineHeader.html)|**`open public fun appendOutlineHeader(to: StringBuilder, node: DocumentationNode): Unit`**|
|[appendText](appendText.html)|**`open public fun appendText(to: StringBuilder, text: String): Unit`**|
|[formatBold](formatBold.html)|**`open public fun formatBold(text: String): String`**|
|[formatBreadcrumbs](formatBreadcrumbs.html)|**`open public fun formatBreadcrumbs(items: Iterable<FormatLink>): String`**|
|[formatCode](formatCode.html)|**`open public fun formatCode(code: String): String`**|
|[formatLink](formatLink.html)|**`open public fun formatLink(text: String, location: Location): String`**|
