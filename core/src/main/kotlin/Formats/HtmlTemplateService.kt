package org.jetbrains.dokka

import java.io.File

interface HtmlTemplateService {
    fun appendHeader(to: StringBuilder, title: String?, basePath: File)
    fun appendFooter(to: StringBuilder)

    companion object {
        fun default(css: String? = null): HtmlTemplateService {
            return object : HtmlTemplateService {
                override fun appendFooter(to: StringBuilder) {
                    if (!to.endsWith('\n')) {
                        to.append('\n')
                    }
                    to.appendln("</BODY>")
                    to.appendln("</HTML>")
                }
                override fun appendHeader(to: StringBuilder, title: String?, basePath: File) {
                    to.appendln("<HTML>")
                    to.appendln("<HEAD>")
                    to.appendln("<meta charset=\"UTF-8\">")
                    if (title != null) {
                        to.appendln("<title>$title</title>")
                    }
                    if (css != null) {
                        val cssPath = basePath.resolve(css).toUnixString()
                        to.appendln("<link rel=\"stylesheet\" href=\"$cssPath\">")
                    }
                    to.appendln("</HEAD>")
                    to.appendln("<BODY>")
                }
            }
        }
    }
}


