import org.redundent.kotlin.xml.*
import java.nio.file.Path
import kotlin.io.path.writeText

data class PomData(
    val notation: Notation,
    val description: String,
    val github: GithubRepo,
    val licenseName: String,
    val developers: List<Developer>,
    val dependencies: List<Dependency>,
)

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

fun PomData.writeToDir(target: Path): PomFile {
    val p = target.resolve("${this.notation.artifact}-${this.notation.version}.pom")
    p.writeText(this.toString())
    return PomFile(p)
}