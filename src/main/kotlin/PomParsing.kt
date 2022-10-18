private fun extractElement(text: String, tag: String) =
    Regex("""<\s*$tag\s*>([^<]*)<\s*/\s*$tag\s*>""").find(text).let {
        check(it != null) { "Element $tag not found" }
        it.groups[1]!!.value
    }

@JvmInline
value class PomXml(val xml: String) {
    init {
        require(modelVersion()=="4.0.0") { "Unsupported model version" }
    }
    // https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#minimal-pom
    fun group() = Group(extractElement(xml, "groupId"))
    fun version() = Version(extractElement(xml, "version"))
    fun artifact() = Artifact(extractElement(xml, "artifactId"))
    fun notation() = Notation(group(), artifact(), version())
    private fun modelVersion() = extractElement(xml, "modelVersion")
}
