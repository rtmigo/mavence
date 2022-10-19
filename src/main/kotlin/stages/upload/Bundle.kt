package stages.upload


import maven.Notation
import stages.sign.*
import tools.*
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.*

@JvmInline
value class SonatypeUsername(val string: String)

@JvmInline
value class SonatypePassword(val string: String)

class SignedBundle(val jar: Path, private val tempDir: CloseableTempDir) : Closeable {
    companion object {

        private fun basename(nota: Notation) =
            "${nota.artifact.string}-${nota.version.string}-binks.jar"

        suspend fun fromFiles(src: MavenArtifactWithTempSignatures) =
            CloseableTempDir.init { tempDir ->
                val targetJar = tempDir.path.resolve(basename(src.content.notation))
                val sources = src.content.files + src.signaturesDir.path.listDirectoryEntries()
                Jar.addByNames(
                    sources,
                    targetJar
                )

                SignedBundle(targetJar, tempDir)
            }
    }

    override fun close() {
        tempDir.close()
    }
}

