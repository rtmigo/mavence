import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*

@JvmInline
value class Artifact(val string: String) {
    init {
        require(string.isNotEmpty() && string[0].isLetter())
    }
}

@JvmInline
value class Group(val string: String) {
    init {
        require(string.isNotEmpty() && string[0].isLetter())
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
value class PomFile(val path: Path)

@JvmInline
value class SignatureFile(val path: Path)

@JvmInline
value class MavenUrl(val url: URL)

@JvmInline
value class ProjectRootDir(val path: Path) {
    init {
        assert(path.resolve("gradlew").exists())
    }
}

@JvmInline
value class ArtifactDir(val path: Path) {
    init {
        require(
            path.resolve("build.gradle.kts").exists() ||
                path.resolve("build.gradle").exists()) {
            "build.gradle not found in $path"
        }
    }
}

@JvmInline
value class BuildGradleFile(val path: Path) {
    init {
        require(path.name.startsWith("build.gradle"))
    }
}

@JvmInline
value class GradlewFile(val path: Path) {
    init {
        require(path.name == "gradlew" || path.name=="gradlew.bat")
    }
}

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
    }
}


data class GithubRepo(val owner: String, val repo: String, val branch: String)

data class Developer(val name: String, val email: String?) {
    val nameAndEmail get() = name + (if (email != null) " <$email>" else "")
}

data class ProjectMeta(
    val description: String,
    val license_name: String,
    val github: GithubRepo,
    val devs: List<Developer>,
    val name: String?,
    val homepage: URL?,
)

interface Package {
    val mavenUrl: MavenUrl
    val notation: Notation
    val isSigned: Boolean
    val bundle: JarFile?
}

class PackageImpl(
    override val mavenUrl: MavenUrl,
    override val notation: Notation,
    override val isSigned: Boolean,
    override val bundle: JarFile?,
) : Package

//class StagedPackage: Package

// class ProjectMeta(NamedTuple):
//    """Включает все основные данные, необходимые для POM XML, кроме версии.
//    Это позволит однажды задать объект `Pom` для объекта, а версию выяснять
//    в последний момент - прямо во время билда."""
//    # group: Group
//    # artifact: Artifact
//    description: str
//    license_name: str
//    github: GithubRepo
//    devs: list[Developer]
//    name: str | None = None  # по умолчанию возьмём artifact
//    homepage: str | None = None  # по умолчанию возьмём страницу гитхаба

// class Developer(NamedTuple):
//    name: str
//    email: str
//    id: str | None = None
//    organization_name: str | None = None
//    organization_url: str | None = None
//
//    @property
//    def name_and_email(self) -> str:
//        return self.name + " <" + self.email + ">"

// Artifact = NewType('Artifact', str)
//Group = NewType('Group', str)
//Version = NewType('Version', str)
//
//JarFile = NewType('JarFile', Path)
//PomFile = NewType('PomFile', Path)
//SignatureFile = NewType('SignatureFile', Path)
//
//MavenUrl = NewType('MavenUrl', str)
//StagingMavenUrl = NewType('StagingMavenUrl', MavenUrl)
//
//ProjectRootDir = NewType('ProjectRootDir', Path)
//
//BuildGradleDir = NewType('BuildGradleDir', Path)
//"""directory containing build.gradle or build.gradle.kts file"""
//
//BuildGradleFile = NewType('BuildGradleFile', Path)
//"""build.gradle or build.gradle.kts file"""
//
//GradlewFile = NewType('GradlewFile', Path)