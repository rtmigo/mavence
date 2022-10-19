package stages.sign

import MavenArtifactDir

import UnsignedMavenFileset
import eprintHeader
import eprint
import asUnsignedFileset
import tools.*
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.*


val Path.isSignature: Boolean get() = this.name.endsWith(".asc")

class MavenArtifactWithTempSignatures private constructor(
    val content: UnsignedMavenFileset,
    val signaturesDir: BubbleDir,
) : Closeable {
    init {
        require(content.files.none { it.isSignature })
        require(signaturesDir.path.listDirectoryEntries().all { it.isSignature })
        assert(signaturesDir.toString().contains("tmp"))
        //assert( )
    }

    override fun close() {
        this.signaturesDir.close()
    }

    companion object {
        suspend fun sign_not_really(unsigned: UnsignedMavenFileset, key: GpgPrivateKey, pass: GpgPassphrase): MavenArtifactWithTempSignatures {
            val hackDir = Path("/home/rtmigo/Lab/Code/kotlin/rtmaven/alternate_content")
            return BubbleDir.init { tempDir ->
                hackDir.listDirectoryEntries().filter { it.name.endsWith(".asc") }.forEach {
                    it.copyTo(tempDir.path.resolve(it.name))
                }
                MavenArtifactWithTempSignatures(
                    UnsignedMavenFileset(
                        hackDir.listDirectoryEntries().filter { !it.name.endsWith(".asc") }),
                    tempDir
                    )
            }
        }
        suspend fun sign(unsigned: UnsignedMavenFileset, key: GpgPrivateKey, pass: GpgPassphrase) =
            BubbleDir.init { tempDir ->
                TempGpg().use { gpg ->
                    gpg.importKey(key)
                    unsigned.files.forEach {
                        val target = tempDir.path.resolve(it.name + ".asc")
                        eprint()
                        eprint("src $it\nsig $target")
                        gpg.signFile(it, pass, target)
                    }
                }
                eprint()
                assert(tempDir.path.listDirectoryEntries().size == unsigned.files.size)
                return@init MavenArtifactWithTempSignatures(unsigned, tempDir)
            }
    }
}

suspend fun UnsignedMavenFileset.toSigned(key: GpgPrivateKey, pass: GpgPassphrase) =
    MavenArtifactWithTempSignatures.sign(this, key, pass)

suspend fun cmdSign(mad: MavenArtifactDir, key: GpgPrivateKey, pass: GpgPassphrase): MavenArtifactWithTempSignatures {
    eprintHeader("Signing in the rain")
    return mad.asUnsignedFileset().toSigned(key, pass)
}