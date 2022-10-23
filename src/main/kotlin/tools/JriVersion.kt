package tools

/** Returns the version of runtime running this program.*/
fun jriVersion(): Int {
    // https://stackoverflow.com/a/2591122/11700241
    var version = System.getProperty("java.version")
    if (version.startsWith("1.")) {
        version = version.substring(2, 3)
    } else {
        val dot = version.indexOf(".")
        if (dot != -1) {
            version = version.substring(0, dot)
        }
    }
    return version.toInt()
}

fun javaVersionToInt(version: String): Int =
    version.split('.').let {
        if (it.size==1)
            it.single().toInt()
        else if (it.size>=2) {
            if (it[0]=="1")
                it[1].toInt()
            else
                it[0].toInt()
        }
        else
            throw IllegalArgumentException(version)
    }
