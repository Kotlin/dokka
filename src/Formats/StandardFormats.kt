package org.jetbrains.dokka.Formats

import org.jetbrains.dokka.*

class HtmlFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = javaClass<HtmlFormatService>()

    override val outlineServiceClass: Class<out OutlineFormatService>
        get() = javaClass<HtmlFormatService>()

    override val generatorServiceClass: Class<out Generator>
        get() = javaClass<FileGenerator>()
}

class KotlinWebsiteFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = javaClass<KotlinWebsiteFormatService>()

    override val outlineServiceClass: Class<out OutlineFormatService>
        get() = javaClass<YamlOutlineService>()

    override val generatorServiceClass: Class<out Generator>
        get() = javaClass<FileGenerator>()
}

class JekyllFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = javaClass<JekyllFormatService>()

    override val outlineServiceClass: Class<out OutlineFormatService>?
        get() = null

    override val generatorServiceClass: Class<out Generator>
        get() = javaClass<FileGenerator>()
}

class MarkdownFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>
        get() = javaClass<MarkdownFormatService>()

    override val outlineServiceClass: Class<out OutlineFormatService>?
        get() = null

    override val generatorServiceClass: Class<out Generator>
        get() = javaClass<FileGenerator>()
}
