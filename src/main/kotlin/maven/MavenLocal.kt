package maven

import org.jsoup.Jsoup
import java.nio.file.*
import kotlin.io.path.*

val m2: Path = Paths.get(System.getProperty("user.home") + "/.m2")
val m2str = m2.toString()


data class MetadataLocalXmlFile(val file: Path) {
    init {
        require(file.name == "maven-metadata-local.xml")
    }

    private val parsed by lazy { Jsoup.parse(file.readText()) }

    val latest get() =
        Version(parsed.select("metadata > versioning > latest").single().text())

    val lastUpdated get() =
        parsed.select("metadata > versioning > lastUpdated").single().text()
}

fun GroupArtifact.expectedLocalXmlFile(ga: GroupArtifact): MetadataLocalXmlFile =
    MetadataLocalXmlFile(
        Paths.get(
            (listOf(m2, "repository") + ga.segments() + listOf("maven-metadata-local.xml"))
                .joinToString("/")))