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


@Deprecated("Seems obsolete", ReplaceWith(""))
data class GithubRepo(val owner: String, val repo: String) {
    fun mainPage(): URL = URL("https://github.com/$owner/$repo")
    fun license(): URL = URL("https://github.com/$owner/$repo/blob/HEAD/LICENSE")
    fun scm(): String = "scm:git://github.com/$owner/$repo.git"
}

@Deprecated("Seems obsolete", ReplaceWith(""))
data class Developer(val name: String, val email: String?) {
    val nameAndEmail get() = name + (if (email != null) " <$email>" else "")

    companion object {
        fun parse(text: String): Developer {
            val m = Regex("([^<]+)(<.+>)?").matchEntire(text)
            require(m != null)
            return Developer(
                m.groups[1]!!.value.trim(),
                m.groups[2]?.value?.trim()?.trim('<', '>')
            )
        }
    }

}

@Deprecated("Seems obsolete", ReplaceWith(""))
data class ProjectMeta(
    val description: String,
    val license_name: String,
    val github: GithubRepo,
    val devs: List<Developer>,
    val name: String?,
    val homepage: URL?,
)

@Deprecated("Seems obsolete", ReplaceWith(""))
interface Package {
    val mavenUrl: MavenUrl
    val notation: Notation
    val isSigned: Boolean
    val bundle: JarFile?
}

@Deprecated("Seems obsolete", ReplaceWith(""))
class PackageImpl(
    override val mavenUrl: MavenUrl,
    override val notation: Notation,
    override val isSigned: Boolean,
    override val bundle: JarFile?,
) : Package

