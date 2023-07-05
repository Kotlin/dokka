# Descriptors: IDE

An internal module that encapsulates external IDE (`org.jetbrains.kotlin:idea`) dependencies.

IDE artifacts are reused for things that are not possible to do with the Kotlin compiler API, such
as KDoc or KLib parsing/processing, because Dokka is very similar to an IDE when it comes to analyzing
source code and docs.

Exists primarily to make sure that unreliable and coupled external dependencies are somewhat abstracted away,
otherwise everything gets tangled together and breaking changes in such dependencies become very
difficult to resolve.
