/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package maven

import FailedToParseValueException
import ValueException
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

    //fun splitSlash() = this.string.replace('.', '/')

    fun segments() = this.string.split('.')
}

@JvmInline
value class Version(val string: String) {
    init {
        require(string.isNotEmpty() && string[0].isDigit()) { "Illegal version: '$string'" }
    }
}

data class GroupArtifact(val group: Group, val artifact: Artifact) {
    companion object {
        fun parse(text: String): GroupArtifact {
            try {
                val parts = text.split(':')
                require(parts.size == 2)
                return GroupArtifact(Group(parts[0]), Artifact(parts[1]))
            } catch (e: Throwable) {
                throw ValueException("group:artifact", text)
            }
        }
    }

    fun segments() = this.group.segments() + listOf(this.artifact.string)

    //fun splitSlash() = "${group.splitSlash()}/${artifact.string}"
    override fun toString(): String = "${group.string}:${artifact.string}"
}

data class Notation(
    val group: Group,
    val artifact: Artifact,
    val version: Version,
) {
    companion object {
        fun parse(text: String): Notation {
            try {
                val parts = text.split(':')
                return Notation(Group(parts[0]), Artifact(parts[1]), Version(parts[2]))
            } catch (e: Throwable) {
                throw ValueException("group:artifact:version", text)
            }
        }
    }

    override fun toString(): String = "${group.string}:${artifact.string}:${version.string}"
}