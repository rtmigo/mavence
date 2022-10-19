import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import stages.build.*
import tools.PomXml
import java.nio.file.Paths
import kotlin.io.path.*

suspend fun cmdLocal(isFinal: Boolean = false): MavenArtifactDir {
    eprintHeader("Publishing to $m2str")
    val f = Paths.get(".")
        .toGradlew().publishAndDetect(null)
    eprint()

    val mad = f.toMavenArtifactDir()

    fun debugReplace(fn: String) {
        // In case Sonatype freezes or gives uninformative errors, we can try to debug by
        // replacing files one at a time. Normally it does not run
        val src = Paths.get("...")
        println("HACKY REPLACE $fn")
        val srcFile = src.resolve(fn)
        val dstFile = mad.path.resolve(fn)
        assert(srcFile.exists())
        assert(dstFile.exists())
        srcFile.copyTo(dstFile, true)
    }

    PomXml(mad.asUnsignedFileset().pomFile.readText()).validate()

    eprint()
    eprint(mad.asUnsignedFileset().pomFile.readText())
    eprint()
    eprint("Artifact dir: ${mad.path}")
    eprint("Notation:     ${mad.asUnsignedFileset().notation}")

    if (isFinal) {
        val nota = mad.asUnsignedFileset().notation
        eprint()
        println(
            Json.encodeToString(
                BuildStdout(
                    group = nota.group.string,
                    version = nota.version.string,
                    artifact = nota.artifact.string,
                    notation = nota.toString(),
                    mavenUrl = "file://$m2str"
                )
            ))
    }

    return mad
}

@Serializable
private data class BuildStdout(
    val group: String,
    val artifact: String,
    val version: String,
    val notation: String,
    val mavenUrl: String
)