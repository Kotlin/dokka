[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [MarkdownFormatService](index.md)

# MarkdownFormatService

```
public class MarkdownFormatService
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|`public MarkdownFormatService(locationService: LocationService, signatureGenerator: SignatureGenerator)`<br>|
|[extension](extension.md)|`open val extension: String`<br>|
|[format](format.md)|`open fun format(nodes: Iterable<DocumentationNode>, to: StringBuilder): Unit`<br>|
|[formatLocation](formatLocation.md)|`private fun StringBuilder.formatLocation(nodes: Iterable<DocumentationNode>): Unit`<br>|
|[formatSummary](formatSummary.md)|`private fun StringBuilder.formatSummary(nodes: Iterable<DocumentationNode>): Unit`<br>|
|[locationService](locationService.md)|`val locationService: LocationService`<br>|
|[signatureGenerator](signatureGenerator.md)|`val signatureGenerator: SignatureGenerator`<br>|
