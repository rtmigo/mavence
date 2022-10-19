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

    private fun required(selector: String) =
        try {
            soup.select(selector).single().text().also {
                check(it.isNotBlank()) //{ "WTF" }
            }
        } catch (e: Throwable) {
            throw Exception("POM problem with item '$selector'.", e)
        }

    private fun requireAtLeastOne(selector: String) =
        try {
            val items = soup.select(selector)
            check(items.size >= 1)
            check(items.all { it.text().isNotBlank() })
        } catch (e: Throwable) {
            throw Exception("POM problem with item '$selector'.", e)
        }

    fun validate() {
        // Отсутствие некоторых элементов в схеме может привести к тому, что Sonatype Staging будет
        // принимать файл, а потом зависать, так никогда и не делая репозиторий доступным. Это
        // конечно большая загадка, в каких ЕЩЁ случаях он так сделает. Но тут можно хоть что-то
        // предотвратить.

        this.group()
        this.version()
        this.artifact()
        this.description()
        this.name()
        this.scmConnection()
        this.scmUrl()
        this.url()

        requireAtLeastOne("project > licenses > license > name")
        requireAtLeastOne("project > licenses > license > url")
        requireAtLeastOne("project > developers > developer > name")
        requireAtLeastOne("project > developers > developer > email")
    }

    fun group() = Group(required("project > groupId"))

    fun version() = Version(required("project > version"))
    fun artifact() = Artifact(required("project > artifactId"))
    fun description() = required("project > description")
    fun name() = required("project > name")
    fun url() = required("project > url")
    fun scmConnection() = required("project > scm > connection")
    fun scmUrl() = required("project > scm > url")

    fun notation() = Notation(group(), artifact(), version())
    private fun modelVersion() = required("project > modelVersion")
}
