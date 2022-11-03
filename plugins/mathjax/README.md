# MathJax plugin

[MathJax](https://docs.mathjax.org/) allows you to include mathematics in your web pages. `MathJax` Dokka plugin
adds the ability to render mathematics from source code comments.

If MathJax plugin encounters `@usesMathJax` KDoc tag, it adds `MathJax.js` (ver. 2) with `config=TeX-AMS_SVG`
to generated `HTML` pages.

Usage example:
```kotlin
/**
 * Some math \(\sqrt{3x-1}+(1+x)^2\)
 * 
 * @usesMathJax
 */
class Foo {}
```

Note that `@usesMathJax` tag is case-sensitive.

MathJax plugin is published to maven central as a
[separate artifact](https://mvnrepository.com/artifact/org.jetbrains.dokka/mathjax-plugin):
```text
org.jetbrains.dokka:mathjax-plugin:1.7.20
```
