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
fun MmdlxFile.toMavenArtifactDir(): MavenArtifactDir =
    MavenArtifactDir(this.file.toPath().parent.listDirectoryEntries()
               .single { it.isDirectory() })

/// Каталог, в котором POM и JARы
data class MavenArtifactDir(val path: Path) {
    val files = path.listDirectoryEntries()

    init {
        check(files.filter { it.name.endsWith(".pom") }.size == 1)
        check(files.any { it.name.endsWith(".jar") })
    }
}