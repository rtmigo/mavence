/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

import maven.*
import org.jsoup.Jsoup
import stages.sign.isSignature
import tools.*
import java.io.File
import java.nio.file.*
import kotlin.io.path.*

private fun listMavenMetadataLocals(): Sequence<MetadataLocalXmlFile> {
    val home = File(System.getProperty("user.home"))
    val m2 = home.resolve(".m2")
    check(m2.exists()) { "$m2 not found" }
    return m2.walk().filter { it.name == "maven-metadata-local.xml" && it.isFile }
        .map { MetadataLocalXmlFile(it.absoluteFile.toPath()) }
}

@JvmInline
value class MavenArtifactDir(val path: Path) {
    init {
        try {
            path.listDirectoryEntries().single { it.name.endsWith(".pom") }
        } catch (e: Throwable) {
            throw IllegalArgumentException("Directory $path does not contain a single POM file.")
        }
    }
}

private fun readLastUpdated(mavenMetaDataLocal: MetadataLocalXmlFile): Long =
    Regex("<lastUpdated>(\\d+)</lastUpdated>")
        .find(mavenMetaDataLocal.file.readText())!!.let {
            it.groupValues[1].toLong()
        }

fun readUpdateTimes(): Map<MetadataLocalXmlFile, Long> =
    listMavenMetadataLocals()
        .map { it to readLastUpdated(it) }
        .toMap()

fun MetadataLocalXmlFile.toMavenArtifactDir() =
    MavenArtifactDir(this.file.parent.resolve(this.latest.string))

fun MavenArtifactDir.asUnsignedFileset() = UnsignedMavenFileset(
    this.path.listDirectoryEntries().filter { !it.isSignature }
)

open class UnsignedMavenFileset(val files: List<Path>) {

    init {
        rethrowingState({ "Single '*-javadoc.jar' not found in $files." },
                        { files.single { it.name.endsWith("-javadoc.jar") } })
        rethrowingState({ "Single '*-sources.jar' not found in $files." },
                        { files.single { it.name.endsWith("-sources.jar") } })
    }

    val pomFile =
        rethrowingState({ "Cannot find POM file in $files" },
                        { files.single { it.name.endsWith(".pom") } })


    val notation by lazy {
        rethrowingState({ "Failed to extract the group:artifact:version from the POM file." },
                        {PomXml(pomFile.readText()).notation()})
    }
}