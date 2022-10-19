package tools

import java.io.Closeable
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createTempDirectory

/** Temporary directory that is automatically removed when object closed. */
class BubbleDir(val path: Path) : Closeable {
    init {
        require(this.path.toString().contains("tmp"))
    }

    override fun close() {
        assert(this.path.toString().contains("tmp"))
        this.path.toFile().deleteRecursively()
    }

    companion object {
        // этот каталог и по умолчанию создаётся с правами достаточно секьюрными для GPG
        // (переключать в 700 не нужно)
        fun createJustDir() = createTempDirectory(
            "tmp",
//            PosixFilePermissions.asFileAttribute(
//                PosixFilePermissions.fromString("rwx------")
//            )
        )

        fun justDelete(p: Path) {
            assert(p.toString().contains("tmp"))
            p.toFile().deleteRecursively()
        }

        inline fun <R> init(block: (dir: BubbleDir) -> R): R {
            val dir = BubbleDir(createJustDir())
            try {
                return block(dir)
            } catch (e: Throwable) {
                dir.close()
                throw e
            }
        }
    }
}