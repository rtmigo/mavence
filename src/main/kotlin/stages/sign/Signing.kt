package stages.sign

import MavenArtifactDir

import UnsMavenArtifactFiles
import eprintHeader
import eprint
import reanUnsigned
import tools.*
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.*


val Path.isSignature: Boolean get() = this.name.endsWith(".asc")

class MavenArtifactWithTempSignatures private constructor(
    val content: UnsMavenArtifactFiles,
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
        suspend fun sign(unsigned: UnsMavenArtifactFiles, key: GpgPrivateKey, pass: GpgPassphrase) =
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

suspend fun UnsMavenArtifactFiles.toSigned(key: GpgPrivateKey, pass: GpgPassphrase) =
    MavenArtifactWithTempSignatures.sign(this, key, pass)

suspend fun cmdSign(mad: MavenArtifactDir, key: GpgPrivateKey, pass: GpgPassphrase): MavenArtifactWithTempSignatures {
    eprintHeader("Signing files in ${mad.path}")
    return mad.reanUnsigned().toSigned(key, pass)
}