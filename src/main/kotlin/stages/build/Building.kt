import stages.build.*
import java.nio.file.Paths

suspend fun cmdLocal(): MavenArtifactDir {
    eprintHeader("Publishing to $m2str")
    val f = Paths.get(".")
        .toGradlew().publishAndDetect(null)
    eprint()

    val mad = f.toMavenArtifactDir()
    eprint("Artifact dir: ${mad.path}")
    eprint("Notation:     ${mad.reanUnsigned().notation}")
    return mad
}