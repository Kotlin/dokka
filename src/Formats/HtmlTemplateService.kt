package org.jetbrains.dokka

public trait HtmlTemplateService {
    fun appendHeader(to: StringBuilder, title: String?)
    fun appendFooter(to: StringBuilder)

    default object {
        public fun default(css: String? = null): HtmlTemplateService {
            return object : HtmlTemplateService {
                override fun appendFooter(to: StringBuilder) {
                    to.appendln("</BODY>")
                    to.appendln("</HTML>")
                }
                override fun appendHeader(to: StringBuilder, title: String?) {
                    to.appendln("<HTML>")
                    to.appendln("<HEAD>")
                    if (title != null) {
                        to.appendln("<title>$title</title>")
                    }
                    if (css != null) {
                        to.appendln("<link rel=\"stylesheet\" href=\"$css\">")
                    }
                    to.appendln("</HEAD>")
                    to.appendln("<BODY>")
                }
            }
        }
    }
}


