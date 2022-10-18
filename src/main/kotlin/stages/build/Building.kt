import stages.build.*
import java.nio.file.Paths

suspend fun cmdLocal(): MavenArtifactDir {
    printHeader("Publishing to $m2str")
    val f = Paths.get(".")
        .toGradlew().publishAndDetect(null)
    printerr()

    val mad = f.toMavenArtifactDir()
    printerr("Artifact dir: ${mad.path}")
    printerr("Notation:     ${mad.reanUnsigned().notation}")
    return mad
}