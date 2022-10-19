package stages.build

fun insertPackaging(xmlCode: String): String {
    val packagingElement = "<packaging>jar</packaging>"
    return if (!xmlCode.contains(packagingElement)) {
        val newCode = xmlCode.replace(
            "</modelVersion>",
            "</modelVersion>\n\t$packagingElement\n")
        assert(newCode!=xmlCode)
        newCode
    } else
        xmlCode
}