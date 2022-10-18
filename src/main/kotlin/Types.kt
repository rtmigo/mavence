import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*

@Deprecated("Seems obsolete", ReplaceWith(""))
private fun looksLikeMavenAllowed(s: String): Boolean =
    Regex("""[A-Za-z][A-Za-z0-9_\-.]*""").matchEntire(s)!=null

@JvmInline
value class Artifact(val string: String) {
    init {
        require(looksLikeMavenAllowed(string))
    }
}

@JvmInline
value class Group(val string: String) {
    init {
        require(looksLikeMavenAllowed(string))
    }
}

@JvmInline
value class Version(val string: String) {
    init {
        require(string.isNotEmpty() && string[0].isDigit())
    }
}

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

@JvmInline
value class MavenUrl(val url: URL)

open class ExpectedException(msg: String) : Exception(msg)
class FailedToParseValueException(value: String) : ExpectedException("Failed to parse '${value}'")


data class Notation(
    val group: Group,
    val artifact: Artifact,
    val version: Version,
) {
    companion object {
        fun parse(text: String): Notation {
            val parts = text.split(":")
            if (parts.size != 3)
                throw FailedToParseValueException(text)
            try {
                return Notation(Group(parts[0]), Artifact(parts[1]), Version(parts[2]))
            } catch (_: Exception) {
                throw FailedToParseValueException(text)
            }
        }

//        fun fromPomPath(segments: List<String>): Notation {
//            // /home/user/.m2/repository/io/magrathea/sla/libr/1.2.3-rc1/libr-1.2.3-rc1.pom
//            require(segments.last().endsWith(".pom"))
//            //return Notation(Group(segments[segments.size))
//        }
    }

    override fun toString(): String = "${group.string}:${artifact.string}:${version.string}"
}

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

