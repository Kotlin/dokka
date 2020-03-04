package utils

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.plugability.DokkaPlugin
import java.io.File

class TestOutputWriterPlugin(failOnOverwrite: Boolean): DokkaPlugin() {
    private val writer = TestOutputWriter(failOnOverwrite)

    val testWriter by extending { plugin<DokkaBase>().outputWriter with writer }
}

class TestOutputWriter(private val failOnOverwrite: Boolean): OutputWriter {
    val contents: Map<String, String> get() = _contents

    private val _contents = mutableMapOf<String, String>()

    override fun write(path: String, text: String, ext: String) {
        val fullPath = listOf(path, ext).joinToString(separator = ".")
        _contents.putIfAbsent(fullPath, text)?.also {
            if (failOnOverwrite) throw AssertionError("File $fullPath is being overwritten.")
        }
    }

    override fun writeResources(pathFrom: String, pathTo: String) = write(pathFrom, File(pathTo).readText(), "")
}
