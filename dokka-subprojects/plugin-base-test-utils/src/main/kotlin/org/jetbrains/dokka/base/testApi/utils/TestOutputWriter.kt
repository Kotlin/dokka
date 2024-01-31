/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import java.util.*

public class TestOutputWriterPlugin(failOnOverwrite: Boolean = true) : DokkaPlugin() {
    public val writer: TestOutputWriter = TestOutputWriter(failOnOverwrite)

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    public val testWriter: Extension<OutputWriter, *, *> by extending {
        (dokkaBase.outputWriter
                with writer
                override dokkaBase.fileWriter)
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

public class TestOutputWriter(
    private val failOnOverwrite: Boolean = true
) : OutputWriter {
    public val contents: Map<String, String> get() = _contents
    private val _contents = Collections.synchronizedMap(mutableMapOf<String, String>())

    override suspend fun write(path: String, text: String, ext: String) {
        val fullPath = "$path$ext"
        _contents.putIfAbsent(fullPath, text)?.also {
            if (failOnOverwrite) throw AssertionError("File $fullPath is being overwritten.")
        }
    }

    override suspend fun writeResources(pathFrom: String, pathTo: String) {
        write(pathTo, "*** content of $pathFrom ***", "")
    }
}
