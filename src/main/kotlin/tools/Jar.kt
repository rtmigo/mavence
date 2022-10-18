package tools

import com.github.pgreze.process.*
import java.nio.file.*
import kotlin.io.path.*


object Jar {
    private suspend fun which(what: String): String? =
        process("which", what, stdout = Redirect.CAPTURE).let {
            if (it.resultCode == 0) it.output.joinToString("\n")
            else null
        }

    private fun jarInJavaHome() = System.getenv("JAVA_HOME").let { "$it/bin/jar" }

    private fun jarInSdkman() =
        System.getProperty("user.home") + "/.sdkman/candidates/java/current/bin/jar"


    private var initialized = false
    private var jarExeCache: String? = null

    suspend fun exe(): String {
        if (!initialized) {
            for (path in listOf(which("jar"), jarInJavaHome(), jarInSdkman())) {
                if (path != null && Paths.get(path).exists()) {
                    jarExeCache = path
                    break
                }
            }
        }
        initialized = true
        if (jarExeCache == null)
            throw IllegalStateException("JAR not found")
        return jarExeCache!!
    }
}

suspend fun Jar.addByBasenames(files: List<Path>, target: Path) {
    require(target.name.endsWith(".jar"))
    check(!target.exists())
    check(files.size==files.map { it.name }.distinct().size)
    assert(files.all { it.exists() })

    val fileArgs = files.map { listOf("-C", it.parent.toString(), it.name) }.flatten()

    val allArgs = listOf(this.exe(), "cvf", target.toString()) + fileArgs
    process(command=allArgs.toTypedArray()).unwrap()
    check(target.exists())
}

suspend fun Jar.contents(jar: Path): List<String> {
    require(jar.name.endsWith(".jar"))
    return process(exe(), "tf", jar.toString(), stdout = Redirect.CAPTURE)
        .also {
            check(it.resultCode == 0)
        }.output
}

