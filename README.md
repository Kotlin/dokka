dokka
=====

Dokka is documentation engine for Kotlin, performing the same function as javadoc for Java.

**NOTE**: It is work in progress both on compiler side and this tool. Do not base your business on it. Yet.  

Documentation Model
=====

Dokka uses Kotlin-as-a-service technology to build `code model`, then processes it into `documentation model`.
`Documentation model` is graph of items describing code elements such as classes, packages, functions, etc.

Each node has semantic attached, e.g. Value:name -> Type:String means that some value `name` is of type `String`.

Each reference between nodes also has semantic attached, and there are three of them:

1. Member - reference means that target is member of the source, form tree.
2. Detail - reference means that target describes source in more details, form tree.
3. Link - any link to any other node, free form.

Member & Detail has reverse Owner reference, while Link's back reference is also Link. 

Nodes that are Details of other nodes cannot have Members. 

Rendering Docs
====
When we have documentation model, we can render docs in various formats, languages and layouts. We have some core services:

* FormatService -- represents output format
* LocationService -- represents folder and file layout 
* SignatureGenerator -- represents target language by generating class/function/package signatures from model

Basically, given the `documentation` as a model, we do this:

```kotlin
    val signatureGenerator = KotlinSignatureGenerator() 
    val locationService = FoldersLocationService(arguments.outputDir)
    val markdown = JekyllFormatService(locationService, signatureGenerator)
    val generator = FileGenerator(signatureGenerator, locationService, markdown)
    generator.generate(documentation) 
```

Samples
====
[Dokka docs](http://orangy.github.io/dokka/doc/dokka/index.html) are built with Dokka. Yes, we bootstrap and dogfood :)

Roadmap
=====

#### Formats

Documentation can be generated in various mark-up formats.

* Text -- plain text format
* HTML -- html format, suitable for local browsing
* MD   -- markdown format, and optional Jekyll extension for your GitHub pages

#### Locations

Place documentation in different file structure. All links are relative regardless of structure.

* Single File   -- all documentation is placed in the single file
* Single Folder -- all documentation is in same folder, files are generated per entity
* Folder Tree   -- folders are mirroring package/class/method structure, files are leaf elements
  
#### Languages

Output symbol declarations in different languages.

* Kotlin
* Java  
* JavaScript

#### Features

* Support JavaDoc in Java and Kotlin files
* Support KDoc in Kotlin files

#### KDoc

KDoc is a flavour of markdown with symbol processing extensions.

* \[name\] - link to `name` (markdown style)
* $name - link to `name` (Kotlin string interpolation style), or ${java.lang.String} for longer references
* $name: - named section, optionally bound to symbol `name`, e.g. param doc
* ${code reference} -- include content of a symbol denoted by reference, e.g. code example

### Building

Build only dokka

```bash
ant fatjar
```

Build dokka and maven plugin

```bash
ant install-fj
cd maven-plugin
mvn install
```

Build dokka and install maven plugin (do not require maven installed)
```bash
ant build-and-install
```
