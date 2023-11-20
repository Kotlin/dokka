# Descriptors: compiler

An internal module that encapsulates external compiler (`org.jetbrains.kotlin:kotlin-compiler`) dependencies.

Parses Kotlin sources.

Exists primarily to make sure that unreliable and coupled external dependencies are somewhat abstracted away,
otherwise everything gets tangled together and breaking changes in such dependencies become very
difficult to resolve.
