package org.jetbrains.dokka.Formats

import org.jetbrains.dokka.*

class HtmlFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = HtmlFormatService::class.java

    override val outlineServiceClass: Class<out OutlineFormatService>
        get() = HtmlFormatService::class.java

    override val generatorServiceClass: Class<out Generator>
        get() = FileGenerator::class.java
}

class KotlinWebsiteFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = KotlinWebsiteFormatService::class.java

    override val outlineServiceClass: Class<out OutlineFormatService>
        get() = YamlOutlineService::class.java

    override val generatorServiceClass: Class<out Generator>
        get() = FileGenerator::class.java
}

class JekyllFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = JekyllFormatService::class.java

    override val outlineServiceClass: Class<out OutlineFormatService>?
        get() = null

    override val generatorServiceClass: Class<out Generator>
        get() = FileGenerator::class.java
}

class MarkdownFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = MarkdownFormatService::class.java

    override val outlineServiceClass: Class<out OutlineFormatService>?
        get() = null

    override val generatorServiceClass: Class<out Generator>
        get() = FileGenerator::class.java
}
