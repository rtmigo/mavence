
import org.jsoup.Jsoup
import stages.sign.isSignature
import tools.*
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
value class MmdlxFile(val file: File) {
    init {
        require(file.name=="maven-metadata-local.xml")
    }

    fun latest() = Version(
        Jsoup.parse(file.readText())
            .select("metadata > versioning > latest").single().text())

}

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
fun MmdlxFile.toMavenArtifactFiles(): OldUnsignedMavenArtifactFiles =
    OldUnsignedMavenArtifactFiles(this.file.toPath().parent.listDirectoryEntries()
                                      .single { it.isDirectory() }.listDirectoryEntries())

fun MmdlxFile.toMavenArtifactDir() =
    MavenArtifactDir(this.file.toPath().parent.resolve(this.latest().string))

fun MavenArtifactDir.reanUnsigned() = UnsMavenArtifactFiles(
    this.path.listDirectoryEntries().filter { !it.isSignature }
)

open class UnsMavenArtifactFiles(val files: List<Path>) {
    val pomFile = files.single { it.name.endsWith(".pom") }
    val notation by lazy { PomXml(pomFile.readText()).notation() }

    val areSigned: Boolean by lazy {
        val signaturesCount = files.filter { it.isSignature }.size
        if (signaturesCount == 0)
            false
        else {
            check(signaturesCount * 2 == this.files.size) { "Unexpected signatures count" }
            true
        }
    }
}

class SignedMavenArtifactFiles(files: List<Path>) : UnsMavenArtifactFiles(files) {
    init {
        require(this.areSigned)
    }
}


data class OldUnsignedMavenArtifactFiles(val files: List<Path>) {
    init {
        check(files.filter { it.name.endsWith(".pom") }.size == 1)
        check(files.any { it.name.endsWith(".jar") })
        check(files.none { it.name.endsWith(".asc") }) // no signatures
    }
}

data class OldSignedMavenArtifactFiles(val files: List<Path>) {
    init {
        check(files.filter { it.name.endsWith(".pom") }.size == 1)
        check(files.any { it.name.endsWith(".jar") })
        // half of files are signatures
        check(
            files.filter { it.name.endsWith(".asc") }.size * 2 ==
                files.size)
    }
}

suspend fun OldUnsignedMavenArtifactFiles.sign(): OldSignedMavenArtifactFiles {
    throw NotImplementedError()
    //TempGpg().importKey(mavenGpgKey())
    return OldSignedMavenArtifactFiles(
        this.files.map {
            throw NotImplementedError()
            //TempGpg().signFile(it, passphrase = mavenGpgPassword())
        })
}





