package org.jetbrains.dokka.javadoc

import com.sun.javadoc.DocErrorReporter
import com.sun.javadoc.SourcePosition
import org.jetbrains.dokka.DokkaLogger

class StandardReporter(val logger: DokkaLogger) : DocErrorReporter {
    override fun printWarning(msg: String?) {
        logger.warn(msg.toString())
    }

    override fun printWarning(pos: SourcePosition?, msg: String?) {
        logger.warn(format(pos, msg))
    }

    override fun printError(msg: String?) {
        logger.error(msg.toString())
    }

    override fun printError(pos: SourcePosition?, msg: String?) {
        logger.error(format(pos, msg))
    }

    override fun printNotice(msg: String?) {
        logger.info(msg.toString())
    }

    override fun printNotice(pos: SourcePosition?, msg: String?) {
        logger.info(format(pos, msg))
    }

    private fun format(pos: SourcePosition?, msg: String?) =
            if (pos == null) msg.toString() else "${pos.file()}:${pos.line()}:${pos.column()}: $msg"
}