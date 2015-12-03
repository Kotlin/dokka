public inline fun <T> T.apply(f: T.() -> Unit): T { f(); return this }
