import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.*


val Path.isSignature: Boolean get() = this.name.endsWith(".asc")

class MavenArtifactWithTempSignatures private constructor(
    val content: UnsMavenArtifactFiles,
    val signaturesDir: Path
) : Closeable {
    init {

        require(content.files.none { it.isSignature })
        assert(signaturesDir.toString().contains("tmp"))
    }

    override fun close() {
        require(signaturesDir.toString().contains("tmp"))
        signaturesDir.toFile().deleteRecursively()
    }

    companion object {
        suspend fun sign(unsigned: UnsMavenArtifactFiles, key: GpgPrivateKey, pass: GpgPassphrase):
            MavenArtifactWithTempSignatures {
            val tempDir = createTempDirectory("signatures_tmp")

            TempGpg().use { gpg ->
                gpg.importKey(key)
                unsigned.files.forEach {
                    val target = tempDir.resolve(it.name + ".asc")
                    printerr()
                    printerr("src $it\nsig $target")
                    gpg.signFile(it, pass, target)
                }
            }
            printerr()

            assert(tempDir.listDirectoryEntries().size == unsigned.files.size)
            return MavenArtifactWithTempSignatures(unsigned, tempDir)
        }
    }
}

suspend fun UnsMavenArtifactFiles.toSigned(key: GpgPrivateKey, pass: GpgPassphrase)
    = MavenArtifactWithTempSignatures.sign(this, key, pass)