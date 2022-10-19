package stages.upload

import Notation
import io.ktor.client.*
import eprintHeader
import eprint
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

suspend fun cmdToStaging(
    mad: MavenArtifactWithTempSignatures,
    user: SonatypeUsername,
    pass: SonatypePassword,
    notation: Notation
): StagingUri {
    eprintHeader("Creating JAR of JARs")
    return SignedBundle.fromFiles(mad).use { bundle ->
        createClient(user, pass).use {
            it.sendToStaging(bundle.jar, notation)
        }
    }
}

suspend fun cmdToRelease(
    uri: StagingUri,
    user: SonatypeUsername,
    pass: SonatypePassword,
) {
    createClient(user, pass).use {
        it.promoteToCentral(uri)
        eprint("HOORAY! We have released the package in Maven Central!")
        eprint("The release may take some time.")
    }
}

