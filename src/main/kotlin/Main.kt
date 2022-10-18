import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import java.nio.file.*

class Database : NoOpCliktCommand(
    name = "rtmaven",
    help = "Maven Central publishing helper."
) {
    init {
        versionOption(Build.version) {
            "$commandName $it (${Build.date})\n" +
                "MIT (c) Artsiom iG <ortemeo@gmail.com>\n" +
                "http://github.com/rtmigo/rtmaven_kt"
        }
    }

    override fun run() = Unit
}

private val m2str = Paths.get(System.getProperty("user.home")).resolve(".m2").toString()

fun printHeader(text: String) {
    val line = List(80) { '%' }.joinToString("")
    System.err.println()
    System.err.println(line)
    System.err.println("  $text")
    System.err.println(line)
    System.err.println()
}

fun printerr(s: String) = System.err.println(s)
fun printerr() = System.err.println("")

suspend fun cmdLocal(): MavenArtifactDir {
    printHeader("Publishing to $m2str")
    val f = Paths.get(".")
        .toGradlew().publishAndDetect(null)
    printerr()

    val mad = f.toMavenArtifactDir()
    printerr("Artifact dir: ${mad.path}")
    printerr("Notation:     ${mad.reanUnsigned().notation}")
    return mad
}

suspend fun cmdSign(mad: MavenArtifactDir, key: GpgPrivateKey, pass: GpgPassphrase) {
    printHeader("Signing files in ${mad.path}")
    val unsignedFiles = mad.reanUnsigned().toSigned(key, pass)

    //check(!unsignedFiles.areSigned) { "Already signed" }

//    TempGpg().use { gpg->
//        gpg.importKey(key)
//        unsignedFiles.files.forEach { gpg.signFile(it, pass) }
//    }


}

class Local : CliktCommand(help = "Build, publish to $m2str") {
    override fun run() = runBlocking {
        cmdLocal()
        Unit
    }
}

open class Signed(help: String = "Build, sign, publish to $m2str") :
    CliktCommand(help = help) {
    private val gpgKey by option("--gpg-key", envvar = "MAVEN_GPG_KEY").required()
    private val gpgPwd by option("--gpg-password", envvar = "MAVEN_GPG_PASSWORD").required()
    override fun run() = runBlocking {
        cmdSign(
            cmdLocal(),
            key=GpgPrivateKey(gpgKey),
            pass =GpgPassphrase(gpgPwd)
        )
    }
}

open class Stage(help: String = "Build, sign, publish to OSSRH Staging") : Signed(help = help) {
    private val sonatypeUser by option(
        "--sonatype-username",
        envvar = "SONATYPE_USERNAME").required()
    private val sonatypePassword by option(
        "--sonatype-password",
        envvar = "SONATYPE_PASSWORD").required()

    override fun run() {
        echo("Initialized the database.")
    }
}

class Central : CliktCommand(help = "Build, sign, publish to OSSRH Staging, release to Central") {
    override fun run() {
        echo("Send to OSSRH and release to Central")
    }
}

suspend fun prepare(dir: Path = Paths.get(".")) {
    dir.toGradlew().publishAndDetect("central")
}

suspend fun main(args: Array<String>) {
    Database()
        .subcommands(Local(), Signed(), Stage(), Central())
        .main(args)
}