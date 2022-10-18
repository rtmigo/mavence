package stages.build

import ArtifactDir
import BuildGradleFile
import ExpectedException
import GradlewFile
import Group
import MmdlxFile
import Notation
import ProjectRootDir
import com.aballano.mnemonik.memoizeSuspend
import com.github.pgreze.process.*
import readUpdateTimes
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists

fun BuildGradleFile.toArtifactDir(): ArtifactDir = ArtifactDir(this.path.parent)

fun Path.selfAndParents() = sequence<Path> {
    var p = this@selfAndParents.absolute()
    require(p.exists())
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
            directory = this.path.toFile()
        )
    }
    check(result.resultCode == 0)
}

suspend fun GradlewFile.getVersion(): String {
    val res = process(
        this.path.toString(), "--version",
        stdout = Redirect.CAPTURE
    )
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
        directory = this.path.parent.toFile()
    )
        .also { check(it.resultCode == 0) }

    check(this.getVersion() == newVersion) { "Failed to change version" }
}

/// Returns all the keys that were added or removed, and also all the keys associated
/// with values that were changed.
internal fun <K, V> keysOfChanges(old: Map<K, V>, new: Map<K, V>): List<K> =
    (old.keys + new.keys).filter { (old[it] ?: 0) != (new[it] ?: 0) }

suspend fun GradlewFile.publishAndDetect(publicationName: String?): MmdlxFile {
    val old = readUpdateTimes()
    this.publishLocal(publicationName)
    val new = readUpdateTimes()
    val changed = keysOfChanges(old, new)
    if (changed.size != 1)
        throw Exception("Cannot detect what is published. Files updated: $changed")
    return changed[0]
}

// publishCentralPublicationToMavenLocal
// publishToMavenLocal
suspend fun GradlewFile.publishLocal(publicationName: String?) {
    println("Publish local")
    process(
        this.path.toString(), this.publishTaskName(publicationName),
        directory = this.path.parent.toFile()).unwrap()
    //stderr = Redirect.Consume { it.collect { System.out.print(it) } },
    //stdout = Redirect.Consume { it.collect { System.err.println(it) } }
//    ).also {
//        check(it.resultCode == 0)
//    }
}

internal suspend fun GradlewFile.publishTaskName(publicationName: String?): String {
    val taskToFindLC = (
        if (publicationName == null)
            "publishToMavenLocal"
        else
            "publish${publicationName}PublicationToMavenLocal")
        .lowercase()

    val tasks = this.tasks()
    val result = tasks.singleOrNull { it.lowercase() == taskToFindLC }
    check(result != null) {
        if (publicationName != null)
            "Gradle task for MavenPublication named '$publicationName' not found."
        else
            "Gradle task for MavenPublication not found."
    }
    return result
}

private suspend fun GradlewFile.tasks(): List<String> =
    process(
        this.path.toString(), "tasks",
        directory = this.path.parent.toFile(),
        stdout = Redirect.CAPTURE
    )
        .let {
            check(it.resultCode == 0)
            it.output
        }.map { it.split(" ", limit = 3) }
        .filter { it.size >= 3 && it[1] == "-" }
        .map { it[0] }.also {
            check(it.contains("build"))
        }


private suspend fun gradleProperties(d: ArtifactDir): Map<String, String> =
    d.path.toGradlew().let {
        process(
            it.path.toString(),
            "-q", "properties",
            directory = it.path.parent.toFile(),
            stdout = Redirect.CAPTURE
        )
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