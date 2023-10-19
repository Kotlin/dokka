#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:3.5.2")
@file:DependsOn("me.alllex.parsus:parsus-jvm:0.4.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")

import Release_main.SemVer.Companion.SemVer
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.alllex.parsus.parser.*
import me.alllex.parsus.token.literalToken
import me.alllex.parsus.token.regexToken

try {
  Release.main(args)
  exitProcess(0)
} catch (ex: Exception) {
  println("${ex::class.simpleName}: ${ex.message}")
  exitProcess(1)
}

/**
 * Release a new version.
 *
 * Requires:
 * * [gh cli](https://cli.github.com/manual/gh)
 * * [kotlin](https://kotlinlang.org/docs/command-line.html)
 * * [git](https://git-scm.com/)
 */
// based on https://github.com/apollographql/apollo-kotlin/blob/v4.0.0-dev.2/scripts/release.main.kts
object Release : CliktCommand() {
  private val skipGitValidation by option(
    "--skip-git-validation",
    help = "skips git status validation"
  ).flag(default = false)

  override fun run() {
    echo("Current Dokkatoo version is $dokkatooVersion")
    echo("git dir is ${Git.rootDir}")

    val startBranch = Git.currentBranch()

    validateGitStatus(startBranch)

    val releaseVersion = semverPrompt(
      text = "version to release?",
      default = dokkatooVersion.copy(snapshot = false),
    ) {
      if (it.snapshot) {
        echo("versionToRelease must not be a snapshot version, but was $it")
      }
      !it.snapshot
    }
    val nextVersion = semverPrompt(
      text = "post-release version?",
      default = releaseVersion.incrementMinor(snapshot = true),
    )
    updateVersionCreatePR(releaseVersion)

    // switch back to the main branch
    Git.switch(startBranch)
    Git.pull(startBranch)

    // Tag the release
    createAndPushTag(releaseVersion)

    confirm("Publish plugins to Gradle Plugin Portal?", abort = true)
    Gradle.publishPlugins()

    // Bump the version to the next snapshot
    updateVersionCreatePR(nextVersion)

    // Go back and pull the changes
    Git.switch(startBranch)
    Git.pull(startBranch)

    echo("Released version $releaseVersion")
  }

  private fun validateGitStatus(startBranch: String) {
    if (skipGitValidation) {
      echo("skipping git status validation")
      return
    }
    check(Git.status().isEmpty()) {
      "git repo is not clean. Stash or commit changes before making a release."
    }
    check(dokkatooVersion.snapshot) {
      "Current version must be a SNAPSHOT, but was $dokkatooVersion"
    }
    check(startBranch == "main") {
      "Must be on the main branch to make a release, but current branch is $startBranch"
    }
  }

  /**
   * @param[validate] returns `null` if the provided SemVer is valid, or else an error message
   * explaining why it is invalid.
   */
  private tailrec fun semverPrompt(
    text: String,
    default: SemVer,
    validate: (candidate: SemVer) -> Boolean = { true },
  ): SemVer {
    val response = prompt(
      text = text,
      default = default.toString(),
      requireConfirmation = true,
    ) {
      SemVer.of(it)
    }

    return if (response == null || !validate(response)) {
      if (response == null) echo("invalid SemVer")
      semverPrompt(text, default, validate)
    } else {
      response
    }
  }

  private fun updateVersionCreatePR(version: SemVer) {
    // checkout a release branch
    val releaseBranch = "release/v$version"
    echo("checkout out new branch...")
    Git.switch(releaseBranch, create = true)

    // update the version & run tests
    dokkatooVersion = version
    echo("running Gradle check...")
    Gradle.check()

    // commit and push
    echo("committing...")
    Git.commit("release $version")
    echo("pushing...")
    Git.push(releaseBranch)

    // create a new PR
    echo("creating PR...")
    GitHub.createPr(releaseBranch)

    confirm("Merge the PR for branch $releaseBranch?", abort = true)
    mergeAndWait(releaseBranch)
    echo("$releaseBranch PR merged")
  }

  private fun createAndPushTag(version: SemVer) {
    // Tag the release
    require(dokkatooVersion == version) {
      "tried to create a tag, but project version does not match provided version. Expected $version but got $dokkatooVersion"
    }
    val tagName = "v$version"
    Git.tag(tagName)
    confirm("Push tag $tagName?", abort = true)
    Git.push(tagName)
    echo("Tag pushed")

    confirm("Publish plugins to Gradle Plugin Portal?", abort = true)
    Gradle.publishPlugins()
  }

  private val buildGradleKts: File by lazy {
    val rootDir = Git.rootDir
    File("$rootDir/build.gradle.kts").apply {
      require(exists()) { "could not find build.gradle.kts in $rootDir" }
    }
  }

  /** Read/write the version set in the root `build.gradle.kts` file */
  private var dokkatooVersion: SemVer
    get() {
      val rawVersion = Gradle.dokkatooVersion()
      return SemVer(rawVersion)
    }
    set(value) {
      val updatedFile = buildGradleKts.useLines { lines ->
        lines.joinToString(separator = "\n", postfix = "\n") { line ->
          if (line.startsWith("version = ")) {
            "version = \"${value}\""
          } else {
            line
          }
        }
      }
      buildGradleKts.writeText(updatedFile)
    }

  private fun mergeAndWait(branchName: String): Unit = runBlocking {
    GitHub.autoMergePr(branchName)
    echo("Waiting for the PR to be merged...")
    while (GitHub.prState(branchName) != "MERGED") {
      delay(1.seconds)
      echo(".", trailingNewline = false)
    }
  }
}

private abstract class CliTool {

  protected fun runCommand(
    cmd: String,
    dir: File? = Git.rootDir,
    logOutput: Boolean = true,
  ): String {
    val args = parseSpaceSeparatedArgs(cmd)

    val process = ProcessBuilder(args).apply {
      redirectOutput(ProcessBuilder.Redirect.PIPE)
      redirectInput(ProcessBuilder.Redirect.PIPE)
      redirectErrorStream(true)
      if (dir != null) directory(dir)
    }.start()

    val processOutput = process.inputStream
      .bufferedReader()
      .lineSequence()
      .onEach { if (logOutput) println("\t$it") }
      .joinToString("\n")
      .trim()

    process.waitFor(10, MINUTES)

    val exitCode = process.exitValue()

    if (exitCode != 0) {
      error("command '$cmd' failed:\n${processOutput}")
    }

    return processOutput
  }

  private data class ProcessResult(
    val exitCode: Int,
    val output: String,
  )

  companion object {
    private fun parseSpaceSeparatedArgs(argsString: String): List<String> {
      val parsedArgs = mutableListOf<String>()
      var inQuotes = false
      var currentCharSequence = StringBuilder()
      fun saveArg(wasInQuotes: Boolean) {
        if (wasInQuotes || currentCharSequence.isNotBlank()) {
          parsedArgs.add(currentCharSequence.toString())
          currentCharSequence = StringBuilder()
        }
      }
      argsString.forEach { char ->
        if (char == '"') {
          inQuotes = !inQuotes
          // Save value which was in quotes.
          if (!inQuotes) {
            saveArg(true)
          }
        } else if (char.isWhitespace() && !inQuotes) {
          // Space is separator
          saveArg(false)
        } else {
          currentCharSequence.append(char)
        }
      }
      if (inQuotes) {
        error("No close-quote was found in $currentCharSequence.")
      }
      saveArg(false)
      return parsedArgs
    }
  }
}

/** git commands */
private object Git : CliTool() {
  val rootDir = File(runCommand("git rev-parse --show-toplevel", dir = null))

  init {
    require(rootDir.exists()) { "could not determine root git directory" }
  }

  fun switch(branch: String, create: Boolean = false): String {
    return runCommand(
      buildString {
        append("git switch ")
        if (create) append("--create ")
        append(branch)
      }
    )
  }

  fun commit(message: String): String = runCommand("git commit -a -m \"$message\"")
  fun currentBranch(): String = runCommand("git symbolic-ref --short HEAD")
  fun pull(ref: String): String = runCommand("git pull origin $ref")
  fun push(ref: String): String = runCommand("git push origin $ref")
  fun status(): String {
    runCommand("git fetch --all")
    return runCommand("git status --porcelain=v2")
  }

  fun tag(tag: String): String {
    return runCommand("git tag $tag")
  }
}

/** GitHub commands */
private object GitHub : CliTool() {

  init {
    setRepo("adamko-dev/dokkatoo")
  }

  fun setRepo(repo: String): String =
    runCommand("gh repo set-default $repo")

  fun prState(branchName: String): String =
    runCommand("gh pr view $branchName --json state --jq .state", logOutput = false)

  fun createPr(branch: String): String =
    runCommand("gh pr create --head $branch --fill")

  fun autoMergePr(branch: String): String =
    runCommand("gh pr merge $branch --squash --auto --delete-branch")

  fun waitForPrChecks(branch: String): String =
    runCommand("gh pr checks $branch --watch --interval 30")
}

/** GitHub commands */
private object Gradle : CliTool() {

  val gradlew: String

  init {
    val osName = System.getProperty("os.name").lowercase()
    gradlew = if ("win" in osName) "./gradlew.bat" else "./gradlew"
  }

  fun stopDaemons(): String = runCommand("$gradlew --stop")

  fun dokkatooVersion(): String {
    stopDaemons()
    return runCommand("$gradlew :dokkatooVersion --quiet --no-daemon")
  }

  fun check(): String {
    stopDaemons()
    return runCommand("$gradlew check --no-daemon")
  }

  fun publishPlugins(): String {
    stopDaemons()
    return runCommand("$gradlew publishPlugins --no-daemon --no-configuration-cache")
  }
}

private data class SemVer(
  val major: Int,
  val minor: Int,
  val patch: Int,
  val snapshot: Boolean,
) {

  fun incrementMinor(snapshot: Boolean): SemVer =
    copy(minor = minor + 1, snapshot = snapshot)

  override fun toString(): String =
    "$major.$minor.$patch" + if (snapshot) "-SNAPSHOT" else ""

  companion object {
    fun SemVer(input: String): SemVer =
      SemVerParser.parseEntire(input).getOrElse { error ->
        error("provided version to release must be SemVer X.Y.Z, but got error while parsing: $error")
      }

    fun of(input: String): SemVer? =
      SemVerParser.parseEntire(input).getOrElse { return null }

    fun isValid(input: String): Boolean =
      try {
        SemVerParser.parseEntireOrThrow(input)
        true
      } catch (ex: ParseException) {
        false
      }
  }

  private object SemVerParser : Grammar<SemVer>() {
    private val dotSeparator by literalToken(".")
    private val dashSeparator by literalToken("-")

    /** Non-negative number that is either 0, or does not start with 0 */
    private val number: Parser<Int> by regexToken("""0|[1-9]\d*""").map { it.text.toInt() }

    private val snapshot by -dashSeparator * literalToken("SNAPSHOT")

    override val root: Parser<SemVer> by parser {
      val major = number()
      dotSeparator()
      val minor = number()
      dotSeparator()
      val patch = number()
      val snapshot = checkPresent(snapshot)
      SemVer(
        major = major,
        minor = minor,
        patch = patch,
        snapshot = snapshot,
      )
    }
  }
}
