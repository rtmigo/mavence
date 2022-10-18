package stages.upload

import printHeader
import stages.sign.*
import tools.*
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.*

@JvmInline
value class SonatypeUsername(val string: String)

@JvmInline
value class SonatypePassword(val string: String)

class SignedBundle(val jar: Path, private val tempDir: BubbleDir) : Closeable {
    companion object {
        suspend fun fromFiles(src: MavenArtifactWithTempSignatures) =
            BubbleDir.init { tempDir ->
                val targetJar = tempDir.path.resolve("bundle.jar")
                val sources = src.content.files + src.signaturesDir.path.listDirectoryEntries()
                Jar.addByBasenames(
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

suspend fun cmdUpload(
    mad: MavenArtifactWithTempSignatures,
    user: SonatypeUsername,
    pass: SonatypePassword
): Unit {
    printHeader("Creating bundle.jar")
    SignedBundle.fromFiles(mad).use {bundle->
        printHeader("Sending")
        authenticate(user, pass).use {
            it.sendToStaging(bundle.jar)
        }
    }

}

// val client = HttpClient(CIO)