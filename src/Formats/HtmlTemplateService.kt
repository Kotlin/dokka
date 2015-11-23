package org.jetbrains.dokka

import java.nio.file.Path

public interface HtmlTemplateService {
    fun appendHeader(to: StringBuilder, title: String?, basePath: Path)
    fun appendFooter(to: StringBuilder)

    companion object {
        public fun default(css: String? = null): HtmlTemplateService {
            return object : HtmlTemplateService {
                override fun appendFooter(to: StringBuilder) {
                    to.appendln("</BODY>")
                    to.appendln("</HTML>")
                }
                override fun appendHeader(to: StringBuilder, title: String?, basePath: Path) {
                    to.appendln("<HTML>")
                    to.appendln("<HEAD>")
                    if (title != null) {
                        to.appendln("<title>$title</title>")
                    }
                    if (css != null) {
                        val cssPath = basePath.resolve(css)
                        to.appendln("<link rel=\"stylesheet\" href=\"$cssPath\">")
                    }
                    to.appendln("</HEAD>")
                    to.appendln("<BODY>")
                }
            }
        }
    }
}


