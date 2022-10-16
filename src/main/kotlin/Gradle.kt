import com.aballano.mnemonik.memoizeSuspend
import com.github.pgreze.process.*
import java.nio.file.Path
import kotlin.io.path.*

fun BuildGradleFile.toArtifactDir(): ArtifactDir = ArtifactDir(this.path.parent)

fun Path.selfAndParents() = sequence<Path> {
    var p = this@selfAndParents.absolute()
    while (true) {
        yield(p)
        val newP = p.parent
        if (newP == p)
            break
        p = newP
    }
}

fun Path.toGradlew(): GradlewFile {
    for (dir in this.selfAndParents()) {
        val gw = dir.resolve("gradlew")
        if (gw.exists())
            return GradlewFile(gw)
    }
    throw ExpectedException("Cannot file gradlew")
}

fun ArtifactDir.toRootDir(): ProjectRootDir = ProjectRootDir(path.toGradlew().path.parent)

suspend fun GradlewFile.version(): String {
    val res = process(
        this.path.toString(), "--version",
        stdout = Redirect.CAPTURE)
    check(res.resultCode == 0)
    // там много строк, но нам нужна "Gradle 7.4.2"
    return res.output.single { it.startsWith("Gradle ") }
        .split(' ')[1]
        .also {
            assert(it[0].isDigit())
        }
}

private suspend fun gradleProperties(d: ArtifactDir): Map<String, String> =
    d.path.toGradlew().let {
        process(it.path.toString(), "-q", "properties", stdout = Redirect.CAPTURE)
    }.let {
        check(it.resultCode == 0)
        it.output
    }.map { it.split(": ", limit = 2) }.map {
        assert(it.size <= 2)
        if (it.size == 2)
            Pair(it[0], it[1])
        else
            Pair(it[0], "")
    }.toMap()

private val cachedProperties = ::gradleProperties.memoizeSuspend()

suspend fun ArtifactDir.gradleVersion() = cachedProperties(this)["version"]!!