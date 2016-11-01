package org.jetbrains

import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CrossPlatformExec extends AbstractExecTask {
    private static final def windowsExtensions = ['bat', 'cmd', 'exe'];
    private static final def unixExtensions = [null, 'sh'];

    private boolean windows;

    public CrossPlatformExec() {
        super(CrossPlatformExec.class);
        windows = OperatingSystem.current().windows;
    }

    @Override
    @TaskAction
    protected void exec() {
        List<String> commandLine = this.getCommandLine();

        if (!commandLine.isEmpty()) {
            commandLine[0] = findCommand(commandLine[0], windows);
        }

        if (windows) {
            if (!commandLine.isEmpty() && commandLine[0]) {
                commandLine
            }
            commandLine.add(0, '/c');
            commandLine.add(0, 'cmd');
        }

        this.setCommandLine(commandLine);

        super.exec();
    }

    private static String findCommand(String command, boolean windows) {
        command = normalizeCommandPaths(command);
        def extensions = windows ? windowsExtensions : unixExtensions;

        return extensions.findResult(command) { extension ->
            Path commandFile
            if (extension) {
                commandFile = Paths.get(command + '.' + extension);
            } else {
                commandFile = Paths.get(command);
            }

            return resolveCommandFromFile(commandFile, windows);
        };
    }

    private static String resolveCommandFromFile(Path commandFile, boolean windows) {
        if (!Files.isExecutable(commandFile)) {
            return null;
        }
        
        return commandFile.toAbsolutePath().normalize();
    }

    private static String normalizeCommandPaths(String command) {
        // need to escape backslash so it works with regex
        String backslashSeparator = '\\\\';

        String forwardSlashSeparator = '/';

        // escape separator if it's a backslash
        char backslash = '\\';
        String separator = File.separatorChar == backslash ? backslashSeparator : File.separator

        return command
        // first replace all of the backslashes with forward slashes
                .replaceAll(backslashSeparator, forwardSlashSeparator)
        // then replace all forward slashes with whatever the separator actually is
                .replaceAll(forwardSlashSeparator, separator);
    }
}