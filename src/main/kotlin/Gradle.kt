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

suspend fun ArtifactDir.gradleClean() {
    val result = this.path.toGradlew().let {
        process(
            it.path.toString(),
            "clean",
            directory = this.path.toFile())
    }
    check(result.resultCode == 0)
}

suspend fun GradlewFile.getVersion(): String {
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

suspend fun GradlewFile.setVersion(newVersion: String) {
    val oldVersion = this.getVersion()
    if (oldVersion == newVersion) {
        println("Current version is $oldVersion. No need to change.")
        return
    }

    process(
        this.path.toString(),
        "-q", "properties",
        directory = this.path.parent.toFile())
        .also { check(it.resultCode == 0) }

    check(this.getVersion() == newVersion) { "Failed to change version" }
}

private suspend fun gradleProperties(d: ArtifactDir): Map<String, String> =
    d.path.toGradlew().let {
        process(
            it.path.toString(),
            "-q", "properties",
            directory = it.path.parent.toFile(),
            stdout = Redirect.CAPTURE)
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
suspend fun ArtifactDir.group() = Group(cachedProperties(this)["group"]!!)
suspend fun ArtifactDir.artifact() = Group(cachedProperties(this)["archivesBaseName"]!!)

data class Dependency(val notation: Notation, val scope: String)

suspend fun ArtifactDir.dependencies(configuration: String): List<Dependency> {
    val lines = this.path.toGradlew().let {
        process(
            it.path.toString(),
            "-q", "dependencies",
            "--configuration", configuration,
            stdout = Redirect.CAPTURE
        ).also { res -> check(res.resultCode == 0) }.output
    }
    return lines.filter { it.startsWith("\\--- ") || it.startsWith("+--- ") }
        .map {
            Dependency(
                Notation.parse(it.split(' ')[1]),
                "runtime"// todo
            )
        }
}