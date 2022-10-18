import io.kotest.matchers.booleans.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(GpgInternals::class)
class GpgTest {
    @Test
    fun tempDir() = runBlocking {
        lateinit var theDir: Path
        TempGpg().use {
            theDir = it.tempHome
            theDir.toString().contains("tmp")
            theDir.exists().shouldBeTrue()
            it.getHome()
        }
        theDir.exists().shouldBeFalse()
    }

    @Test
    fun sign() = runBlocking {
        val testerPrivateKey = """
            -----BEGIN PGP PRIVATE KEY BLOCK-----
            
            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
            Sq3CDVWvIhX8Hk+2elQ0
            =jojx
            -----END PGP PRIVATE KEY BLOCK-----            
        """.trimIndent()

            val tempDir = createTempDirectory("temp")
            try {
                val fileToSign = tempDir.resolve("file.txt")
                val expectedSignature = tempDir.resolve("file.txt.asc")
                signFile(fileToSign,
                         GpgPrivateKey(testerPrivateKey),
                         GpgPassphrase("password123"))
//                fileToSign.writeText("Hello, GPG")
//                println("Signing...")
//                it.signFile(fileToSign, GpgPassphrase("password123"))
                expectedSignature.readText().trim()
                    .startsWith("-----BEGIN PGP SIGNATURE-----")
                    .shouldBeTrue()
//                println("Signed")
            } finally {
                require(tempDir.toString().contains("temp"))
                tempDir.toFile().deleteRecursively()
            }



//        TempGpg().use {
//            it.importKey(GpgPrivateKey(testerPrivateKey))
//
//            val tempDir = createTempDirectory("temp")
//            try {
//                val fileToSign = tempDir.resolve("file.txt")
//                val expectedSignature = tempDir.resolve("file.txt.asc")
//                fileToSign.writeText("Hello, GPG")
//                println("Signing...")
//                it.signFile(fileToSign, GpgPassphrase("password123"))
//                expectedSignature.readText().trim()
//                    .startsWith("-----BEGIN PGP SIGNATURE-----")
//                    .shouldBeTrue()
//                println("Signed")
//            } finally {
//                require(tempDir.toString().contains("temp"))
//                tempDir.toFile().deleteRecursively()
//            }
//        }
    }
}