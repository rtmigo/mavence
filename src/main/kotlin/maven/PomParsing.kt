/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package maven

import org.jsoup.Jsoup
import tools.*


data class PomXml(val xml: String) {
    private val soup = Jsoup.parse(xml)

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
            throw Exception("POM error: item '$selector'.", e)
        }

    fun validate(targetCompatibility: Int) {
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

        soup.select("project > licenses > license")
            .also { check(it.size>=1) }
            .forEach { it.select("license > name") }

        requireAtLeastOne("project > licenses > license > name")
        requireAtLeastOne("project > licenses > license > url")
        requireAtLeastOne("project > developers > developer > name")
        requireAtLeastOne("project > developers > developer > email")



        val pomJavaVersion = this.javaVersion()
//        val runningJavaVersion = jriVersion()
//
        if (pomJavaVersion<targetCompatibility) {
            // TODO unit test
            throw Exception("The targetCompatibility Java version is $targetCompatibility, but " +
                                "<java.version/> in POM is $pomJavaVersion. " +
                                "Either target older Java or change the " +
                                "<java.version/> in POM.")
        }
    }

    private val projectElement by lazy {soup.selectXpath("/html/body/project").single() }

    /** Returns `properties > java.version` converted to `Int` or 8. */
    fun javaVersion(): Int = // TODO unit test
        rethrowingState(
            {"POM error: java.version"},
            {
                projectElement.selectXpath("properties/java.version")
                    .firstOrNull()?.text()?.toInt() })
            ?: 8

//    fun mavenCompilerTarget(): Int = mavenCompiler("maven.compiler.target")
//
//    // https://www.baeldung.com/maven-java-version#compiler
//    private fun mavenCompiler(tag: String): Int =
//        rethrowingState(
//            {"POM error: $tag"},
//            {
//                projectElement.selectXpath("properties/$tag")
//                    .firstOrNull()?.text()?.toInt() })
//            ?: 6


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
