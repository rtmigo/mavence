/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package stages.build

import ArtifactDir
import BuildGradleFile
import ExpectedException
import GradlewFile


import ProjectRootDir
import com.aballano.mnemonik.memoizeSuspend
import com.github.pgreze.process.*

import maven.*
import tools.rethrowingState
import java.nio.file.*
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

suspend fun GradlewFile.getGradleVersion(): String {
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

suspend fun GradlewFile.setGradleVersion(newVersion: String) {
    val oldVersion = this.getGradleVersion()
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

    check(this.getGradleVersion() == newVersion) { "Failed to change version" }
}

/// Returns all the keys that were added or removed, and also all the keys associated
/// with values that were changed.
internal fun <K, V> keysOfChanges(old: Map<K, V>, new: Map<K, V>): List<K> =
    (old.keys + new.keys).filter { (old[it] ?: 0) != (new[it] ?: 0) }


suspend fun GradlewFile.publishAndDetect(
    ga: GroupArtifact,
    publicationName: String?
): MetadataLocalXmlFile {
    //val old = readUpdateTimes()
    fun xml() = ga.expectedLocalXmlFile(ga)

    val oldXml = xml()

    val oldLastUpdated = try {
        oldXml.lastUpdated
    } catch (_: Throwable) {
        "none"
    }

    this.publishLocal(publicationName)
    val newXml = xml()

    check(newXml.file.exists()) { "File '${newXml.file} not found'" }
    check(newXml.lastUpdated != oldLastUpdated) {
        "'lastUpdated' value was not changed in ${newXml.file}"
    }

    return newXml
    //val new = readUpdateTimes()
    //val changed = keysOfChanges(old, new)
    //if (changed.size != 1)
    //    throw Exception("Cannot detect what is published. Files updated: $changed")
    //return changed[0]
}

// publishCentralPublicationToMavenLocal
// publishToMavenLocal
suspend fun GradlewFile.publishLocal(publicationName: String?) {
    process(
        this.path.toString(), this.publishTaskName(publicationName),
        directory = this.path.parent.toFile(),
        stdout = Redirect.Consume { it.collect { System.err.println(it) } }
    ).unwrap()
    //stderr = Redirect.Consume { it.collect { System.out.print(it) } },

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


private suspend fun gradleProperties(artidir: ArtifactDir): Map<String, String> =
    artidir.path.toGradlew().let {
        process(
            it.path.toString(),
            "-q", "properties",
            directory = artidir.path.toFile(),
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

suspend fun ArtifactDir.group(): Group = rethrowingState(
    { "Failed to detect the package Group in ${this.path}" },
    { Group(cachedProperties(this)["group"]!!) })

suspend fun ArtifactDir.artifact(): Artifact = rethrowingState(
    { "Failed to detect the Artifact in ${this.path}" },
    { Artifact(cachedProperties(this)["archivesBaseName"]!!) })


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