import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createTempDirectory


object SessionTempDir {
    val tempDir: Path by lazy { createTempDirectory("tmp") }

    private fun recursiveDeleteOnShutdownHook(path: Path) {
        require(path.toString().contains("tmp"))
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                path.toFile().deleteRecursively()
            } catch (e: IOException) {
                throw RuntimeException("Failed to delete $path", e)
            }
        })
    }
}

