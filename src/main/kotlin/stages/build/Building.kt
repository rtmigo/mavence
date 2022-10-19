import stages.build.*
import java.nio.file.Paths
import kotlin.io.path.readText

suspend fun cmdLocal(): MavenArtifactDir {
    eprintHeader("Publishing to $m2str")
    val f = Paths.get(".")
        .toGradlew().publishAndDetect(null)
    eprint()

    val mad = f.toMavenArtifactDir()
    //mad.updateInplace()
//    eprint("HACK: replacing artifact dir")
//    val mad = MavenArtifactDir(Paths.get("/home/rtmigo/Lab/Code/kotlin/rtmaven/alternate_content"))

    eprint("--------------------------------------------------------------------------")
    eprint(mad.asUnsignedFileset().pomFile.readText())
    eprint("--------------------------------------------------------------------------")
    eprint("Artifact dir: ${mad.path}")
    eprint("Notation:     ${mad.asUnsignedFileset().notation}")

    return mad
}