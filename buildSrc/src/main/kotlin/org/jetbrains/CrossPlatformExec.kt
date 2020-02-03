package org.jetbrains

import org.gradle.api.tasks.AbstractExecTask
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

open class CrossPlatformExec : AbstractExecTask<CrossPlatformExec>(CrossPlatformExec::class.java) {
    private val windowsExtensions = listOf(".bat", ".cmd", ".exe")
    private val unixExtensions = listOf("", ".sh")

    private val isWindows = OperatingSystem.current().isWindows

    override fun exec() {
        val commandLine: MutableList<String> = this.commandLine

        if (commandLine.isNotEmpty()) {
            commandLine[0] = findCommand(commandLine[0])
        }

        if (isWindows && commandLine.isNotEmpty() && commandLine[0].isNotBlank()) {
            this.commandLine = listOf("cmd", "/c") + commandLine
        } else {
            this.commandLine = commandLine
        }

        super.exec()
    }

    private fun findCommand(command: String): String {
        val command = normalizeCommandPaths(command)
        val extensions = if (isWindows) windowsExtensions else unixExtensions

        return extensions.map { extension ->
            resolveCommandFromFile(Paths.get("$command$extension"))
        }.firstOrNull() ?: command
    }

    private fun resolveCommandFromFile(commandFile: Path) =
        if (!Files.isExecutable(commandFile)) {
            ""
        } else {
            commandFile.toAbsolutePath().normalize().toString()
        }


    private fun normalizeCommandPaths(command: String): String {
        // need to escape backslash so it works with regex
        val backslashSeparator = "\\"
        val forwardSlashSeparator = "/"

        // get the actual separator
        val separator = if (File.separatorChar == '\\') backslashSeparator else File.separator

        return command
            // first replace all of the backslashes with forward slashes
            .replace(backslashSeparator, forwardSlashSeparator)
            // then replace all forward slashes with whatever the separator actually is
            .replace(forwardSlashSeparator, separator)
    }
}