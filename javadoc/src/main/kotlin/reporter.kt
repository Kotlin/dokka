package org.jetbrains.dokka.javadoc

import com.sun.javadoc.DocErrorReporter
import com.sun.javadoc.SourcePosition

object StandardReporter : DocErrorReporter {
    override fun printWarning(msg: String?) {
        System.err?.println("[WARN] $msg")
    }

    override fun printWarning(pos: SourcePosition?, msg: String?) {
        System.err?.println("[WARN] ${pos?.file()}:${pos?.line()}:${pos?.column()}: $msg")
    }

    override fun printError(msg: String?) {
        System.err?.println("[ERROR] $msg")
    }

    override fun printError(pos: SourcePosition?, msg: String?) {
        System.err?.println("[ERROR] ${pos?.file()}:${pos?.line()}:${pos?.column()}: $msg")
    }

    override fun printNotice(msg: String?) {
        System.err?.println("[NOTICE] $msg")
    }

    override fun printNotice(pos: SourcePosition?, msg: String?) {
        System.err?.println("[NOTICE] ${pos?.file()}:${pos?.line()}:${pos?.column()}: $msg")
    }
}