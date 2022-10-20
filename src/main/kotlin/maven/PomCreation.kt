/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package maven


import PomFile
import org.redundent.kotlin.xml.*
import stages.build.Dependency
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.writeText

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

@Deprecated("Since 2022-10", ReplaceWith(""))
data class PomData(
    val notation: Notation,
    val description: String,
    val github: GithubRepo,
    val licenseName: String,
    val developers: List<Developer>,
    val dependencies: List<Dependency>,
)

@Deprecated("Since 2022-10", ReplaceWith(""))
fun PomData.toXml(): String =
    xml("project") {
        "modelVersion" { -"4.0.0" }
        "packaging" { -"jar" }
        "groupId" { -notation.group.string }
        "artifactId" { -notation.artifact.string }
        "version" { -notation.version.string }
        "name" { -notation.artifact.string }
        "description" { -description }
        "url" { -github.mainPage().toString() }
        "scm" {
            "connection" { -github.scm() }
            "url" { -github.mainPage().toString() }
        }
        "licenses" {
            "license" {
                "name" { -licenseName }
                "url" { -github.license().toString() }
            }
        }
        "developers" {
            developers.distinct().forEach { dev ->
                "dev" {
                    "name" { -dev.name }
                    dev.email?.let { "email" { -it } }
                }
            }
        }
        "dependencies" {
            dependencies.distinct()
                .sortedBy { it.notation.group.string + " " + it.notation.artifact.string }
                .map {
                    "dependency" {
                        "groupId" { -it.notation.group.string }
                        "artifactId" { -it.notation.artifact.string }
                        "version" { -it.notation.version.string }
                        "scope" { -it.scope }
                    }
                }
        }

    }.toString(PrintOptions(singleLineTextElements = true))
        .replace(
            "<project>",
            """<project xsi:schemaLocation=
                |"http://maven.apache.org/POM/4.0.0
                | https://maven.apache.org/xsd/maven-4.0.0.xsd">""".trimMargin())

@Deprecated("Since 2022-10", ReplaceWith(""))
fun PomData.writeToDir(target: Path): PomFile {
    val p = target.resolve("${this.notation.artifact}-${this.notation.version}.pom")
    p.writeText(this.toString())
    return PomFile(p)
}
