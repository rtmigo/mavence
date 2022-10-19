package maven

import FailedToParseValueException
import java.lang.IllegalArgumentException

private fun looksLikeMavenAllowed(s: String): Boolean =
    Regex("""[A-Za-z][A-Za-z0-9_\-.]*""").matchEntire(s) != null

@JvmInline
value class Artifact(val string: String) {
    init {
        require(looksLikeMavenAllowed(string)) { "Illegal artifact name: '$string'" }
    }
}

@JvmInline
value class Group(val string: String) {
    init {
        require(looksLikeMavenAllowed(string)) { "Illegal group name: '$string'" }
    }
}

@JvmInline
value class Version(val string: String) {
    init {
        require(string.isNotEmpty() && string[0].isDigit())  { "Illegal version: '$string'" }
    }
}

data class GroupArtifact(val group: Group, val artifact: Artifact) {
    companion object {
        fun parse(text: String): GroupArtifact {
            try {
                val parts = text.split(':')
                require(parts.size == 2)
                return GroupArtifact(Group(parts[1]), Artifact(parts[1]))
            } catch (e: Throwable) {
                throw IllegalArgumentException("Failed to parse '$text'", e)
            }
        }
    }

    override fun toString(): String = "${group.string}:${artifact.string}"
}

data class Notation(
    val group: Group,
    val artifact: Artifact,
    val version: Version,
) {
    companion object {
        fun parse(text: String): Notation {
            val parts = text.split(':')
            if (parts.size != 3)
                throw FailedToParseValueException(text)
            try {
                return Notation(Group(parts[0]), Artifact(parts[1]), Version(parts[2]))
            } catch (_: Exception) {
                throw FailedToParseValueException(text)
            }
        }
    }

    override fun toString(): String = "${group.string}:${artifact.string}:${version.string}"
}