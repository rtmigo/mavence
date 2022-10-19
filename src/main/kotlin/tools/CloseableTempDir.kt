package tools

import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

/** Temporary directory that is automatically removed when object closed. */
class CloseableTempDir(val path: Path) : Closeable {
    init {
        require(this.path.toString().contains(tmpPrefix))
    }

    override fun close() {
        assert(this.path.toString().contains(tmpPrefix))
        try {
            this.path.toFile().deleteRecursively()
        } catch (e: Throwable) {
            System.err.println(
                "Failed to delete temporary directory $path:\n" +
                    "$e\n" +
                    "${e.stackTrace}")
        }
    }

    companion object {
        const val tmpPrefix = "tmp"

        inline fun <R> init(block: (dir: CloseableTempDir) -> R): R {
            val dir = CloseableTempDir(createTempDirectory(tmpPrefix))
            try {
                return block(dir)
            } catch (e: Throwable) {
                dir.close()
                throw e
            }
        }
    }
}