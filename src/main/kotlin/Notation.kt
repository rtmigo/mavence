private fun looksLikeMavenAllowed(s: String): Boolean =
    Regex("""[A-Za-z][A-Za-z0-9_\-.]*""").matchEntire(s) != null

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

    override fun toString(): String = "${group.string}:${artifact.string}:${version.string}"
}