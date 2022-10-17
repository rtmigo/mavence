private fun envVarOrFail(key: String): String {
    val x = System.getenv(key) ?: ""
    check(x.isNotBlank()) { "Environment variable $key is not set." }
    return x
}

fun sonatypeUsername() = envVarOrFail("SONATYPE_USERNAME")
fun sonatypePassword() = envVarOrFail("SONATYPE_PASSWORD")
fun mavenGpgKey() = GpgPrivateKey(envVarOrFail("MAVEN_GPG_KEY"))
fun mavenGpgPassword() = GpgPassphrase(envVarOrFail("MAVEN_GPG_PASSWORD"))
