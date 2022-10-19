
import org.jsoup.Jsoup
import stages.sign.isSignature
import tools.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

private fun listMavenMetadataLocals(): Sequence<MetadataLocalXmlFile> {
    val home = File(System.getProperty("user.home"))
    val m2 = home.resolve(".m2")
    check(m2.exists()) { "$m2 not found" }
    return m2.walk().filter { it.name == "maven-metadata-local.xml" && it.isFile }
        .map { MetadataLocalXmlFile(it.absoluteFile) }
}

/// maven-metadata-local.xml
@JvmInline
value class MetadataLocalXmlFile(val file: File) {
    init {
        require(file.name=="maven-metadata-local.xml")
    }

    fun latest() = Version(
        Jsoup.parse(file.readText())
            .select("metadata > versioning > latest").single().text())

}

@JvmInline
value class MavenArtifactDir(val path: Path) {
    fun updateInplace() {
        val pomFile = this.asUnsignedFileset().pomFile
        Path("/home/rtmigo/Lab/Code/kotlin/rtmaven/override_pom.hack").copyTo(pomFile, overwrite = true)
        //val pomFile = this.reanUnsigned().pomFile
        //pomFile.writeText(insertPackaging(pomFile.readText()))
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
    MavenArtifactDir(this.file.toPath().parent.resolve(this.latest().string))

fun MavenArtifactDir.asUnsignedFileset() = UnsignedMavenFileset(
    this.path.listDirectoryEntries().filter { !it.isSignature }
)

open class UnsignedMavenFileset(val files: List<Path>) {
    val pomFile = files.single { it.name.endsWith(".pom") }
    val notation by lazy { PomXml(pomFile.readText()).notation() }

//    val areSigned: Boolean by lazy {
//        val signaturesCount = files.filter { it.isSignature }.size
//        if (signaturesCount == 0)
//            false
//        else {
//            check(signaturesCount * 2 == this.files.size) { "Unexpected signatures count" }
//            true
//        }
//    }
}

//class SignedMavenArtifactFiles(files: List<Path>) : UnsignedMavenFileset(files) {
//    init {
//        require(this.areSigned)
//    }
//}


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

suspend fun OldUnsignedMavenArtifactFiles.smiley(): OldSignedMavenArtifactFiles {
    throw NotImplementedError()
    //TempGpg().importKey(mavenGpgKey())
    return OldSignedMavenArtifactFiles(
        this.files.map {
            throw NotImplementedError()
            //TempGpg().signFile(it, passphrase = mavenGpgPassword())
        })
}





