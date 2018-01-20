package p

class Some


fun <T : Some> T.extFun() = ""
val <T : Some> T.extVal get() = ""

fun <T : Some?> T.nullableExtFun() = ""
val <T : Some?> T.nullableExtVal get() = ""