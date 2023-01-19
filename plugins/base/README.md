# Base plugin

The Base plugin comes with a number of basic extensions and features that are likely to be needed by any output format and
some plugins in general. 

Including, but not limited to:

* Markdown and KDoc parsing.
* Kotlin signature generation.
* Transformers that suppress empty modules/packages, unwanted visibility modifiers, obvious functions 
  (hashCode/equals/etc), and so on. 
* Declaration link resolution (i.e linking to a class in a KDoc).
* Support for external documentation links (i.e links to Java's Javadocs website or Kotlin's stdlib).
* Declaration source link generation for navigation to source code (a.k.a `source` button).
* Multiplatform support.
* Output file writers.

The Base plugin is not intended to be used directly by those who want to generate documentation - it is a building block
for other plugins and formats.

It is in the heart of all documentation formats that come with Dokka.
