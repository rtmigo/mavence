import maven.Notation
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*



@JvmInline
value class JarFile(val path: Path)

@JvmInline
value class PomFile(val path: Path) {
    init {
        require(path.name.endsWith(".pom"))
    }
}

@JvmInline
value class SignatureFile(val path: Path)


open class MavenUrl(val url: URL)

fun MavenUrl.toPomUrl(notation: Notation): URL {
    val sb = StringBuilder(this.url.toString().trimEnd('/'))
    sb.append('/')
    sb.append(notation.group.string
                  .replace(':', '/')
                  .replace('.', '/'))
    sb.append('/')
    sb.append(notation.artifact.string)
    sb.append('/')
    sb.append(notation.version.string)
    sb.append('/')
    sb.append(notation.artifact.string+"-"+notation.version.string+".pom")
    return URL(sb.toString())
}

open class ExpectedException(msg: String) : Exception(msg)
class FailedToParseValueException(value: String) : ExpectedException("Failed to parse '${value}'")


