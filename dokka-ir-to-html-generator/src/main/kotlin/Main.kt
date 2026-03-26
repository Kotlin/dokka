/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.File
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java -jar docgen.jar <path-to-input.json>")
        return
    }

    val inputPath = args[0]
    val inputFile = File(inputPath)

    if (!inputFile.exists()) {
        println("Error: Input file not found at ${inputFile.absolutePath}")
        return
    }

    val pageData: Map<String, Any> = mapper.readValue(inputFile)

    println("Starting GraalJS context...")

    // 2. Initialize the context
    // "js.esm-eval-returns-exports" may be useful in the future, but isn't required for a var-library
    val context = Context.newBuilder("js")
        .build()

    // 3. Load the bundle
    // Note: the file will appear there only after npm run build
    val bundleUrl = Thread.currentThread().contextClassLoader.getResource("server-bundle.js")
        ?: throw RuntimeException("server-bundle.js not found! Did you run 'npm run build'?")

    // 4. Execute the bundle
    // We need to define 'global' and 'window' because webpack sometimes references them
    context.eval("js", "var global = this; var window = this; var console = { log: function(x){ print(x + '\\n'); } };")

    try {
        context.eval(Source.newBuilder("js", bundleUrl).build())
    } catch (e: Exception) {
        println("Error evaluating JS bundle: ${e.message}")
        e.printStackTrace()
        return
    }

    // 5. Access the 'SSR' variable exported by Webpack
    val ssr = context.getBindings("js").getMember("SSR")

    if (ssr == null || ssr.isNull) {
        println("Error: 'SSR' variable not found in JS context.")
        return
    }

    // 6. Call the render function
    val renderFunc = ssr.getMember("render")

    // Serialize to JSON on the Kotlin side and parse into a JS object on the GraalJS side.
    val jsonString = toJson(pageData)
    // Get JSON.parse from the context
    val jsonParse = context.getBindings("js").getMember("JSON").getMember("parse")
    // Turn the JSON string into a real JS object
    val jsPageData = jsonParse.execute(jsonString)

    println("Rendering React component...")
    val htmlString = renderFunc.execute(jsPageData).asString()

    // 7. Result
    println("--- HTML OUTPUT ---")
    println(htmlString)
    println("-------------------")

    // Fix the path to the template
    val templateResource = Thread.currentThread().contextClassLoader.getResource("index.html")

    if (templateResource == null) {
        println("Error: Template file 'index.html' not found in JAR resources.")
        return
    }

    val template = templateResource.readText()
    val finalHtml = template
            .replace("<!-- APP_CONTENT_PLACEHOLDER -->", htmlString)
        .replace("<!-- APP_DATA_PLACEHOLDER -->", toJson(pageData))
    File("output.html").writeText(finalHtml)
}

val mapper = jacksonObjectMapper()

fun toJson(data: Any): String {
    return mapper.writeValueAsString(data)
}