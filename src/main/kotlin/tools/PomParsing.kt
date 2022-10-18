package tools

import Artifact
import Group
import Notation
import Version
import org.jsoup.Jsoup

//fun xmlElementText(xml: String, tag: String) =
//    try {
//        Regex("""<\s*$tag\s*>([^<]*)<\s*/\s*$tag\s*>""").findAll(xml).single().let {
//            //check(it != null) { "Element $tag not found" }
//            it.groups[1]!!.value
//        }
//    } catch (e: Throwable) {
//        throw Exception("Failed to find single element with tag '$tag'.", e)
//    }


data class PomXml(val xml: String) {
    val soup = Jsoup.parse(xml)

    init {
        require(modelVersion() == "4.0.0") { "Unsupported model version" }
    }

    // https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#minimal-pom
    fun group() = Group(soup.select("project > groupId").single().text())
    fun version() = Version(soup.select("project > version").single().text())
    fun artifact() = Artifact(soup.select("project > artifactId").single().text())
    fun notation() = Notation(group(), artifact(), version())
    private fun modelVersion() = soup.select("project > modelVersion").single().text()
    //private fun modelVersion() = xmlElementText(xml, "modelVersion")
}
