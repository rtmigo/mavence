import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

private fun listMavenMetadataLocals(): Sequence<MmdlxFile> {
    val home = File(System.getProperty("user.home"))
    val m2 = home.resolve(".m2")
    check(m2.exists()) { "$m2 not found" }
    return m2.walk().filter { it.name == "maven-metadata-local.xml" && it.isFile }
        .map { MmdlxFile(it.absoluteFile) }
}

/// maven-metadata-local.xml
@JvmInline
value class MmdlxFile(val file: File)

@JvmInline
value class MavenArtifactDir(val path: Path)

private fun readLastUpdated(mavenMetaDataLocal: MmdlxFile): Long =
    Regex("<lastUpdated>(\\d+)</lastUpdated>")
        .find(mavenMetaDataLocal.file.readText())!!.let {
            it.groupValues[1].toLong()
        }

fun readUpdateTimes(): Map<MmdlxFile, Long> =
    listMavenMetadataLocals()
        .map { it to readLastUpdated(it) }
        .toMap()

/// maven-metadata-local.xml обычно находится по соседству с единственным каталогом -
/// тем, где сам артифакт
fun MmdlxFile.toMavenArtifactFiles(): UnsignedMavenArtifactFiles =
    UnsignedMavenArtifactFiles(this.file.toPath().parent.listDirectoryEntries()
                                   .single { it.isDirectory() }.listDirectoryEntries())

fun MmdlxFile.toMavenArtifactDir() =
    MavenArtifactDir(this.file.toPath().parent.listDirectoryEntries()
                         .single { it.isDirectory() })

fun MavenArtifactDir.read() = BetterMavenArtifactFiles(this.path.listDirectoryEntries())

data class BetterMavenArtifactFiles(val files: List<Path>) {
    init {
        //println(files)
    }
    val pomFile = files.single { it.name.endsWith(".pom") }
    val notation by lazy { PomXml(pomFile.readText()).notation() }
}


data class UnsignedMavenArtifactFiles(val files: List<Path>) {
    init {
        check(files.filter { it.name.endsWith(".pom") }.size == 1)
        check(files.any { it.name.endsWith(".jar") })
        check(files.none { it.name.endsWith(".asc") }) // no signatures
    }
}

data class SignedMavenArtifactFiles(val files: List<Path>) {
    init {
        check(files.filter { it.name.endsWith(".pom") }.size == 1)
        check(files.any { it.name.endsWith(".jar") })
        // half of files are signatures
        check(
            files.filter { it.name.endsWith(".asc") }.size * 2 ==
                files.size)
    }
}

suspend fun UnsignedMavenArtifactFiles.sign(): SignedMavenArtifactFiles {
    TempGpg().importKey(mavenGpgKey())
    return SignedMavenArtifactFiles(
        this.files.map {
            throw NotImplementedError()
            //TempGpg().signFile(it, passphrase = mavenGpgPassword())
        })
}





