import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import stages.build.*
import stages.sign.cmdSign
import stages.upload.*
import tools.*

import java.nio.file.*

val m2str = Paths.get(System.getProperty("user.home")).resolve(".m2").toString()

fun eprintHeader(text: String) {
    val line = List(80) { '%' }.joinToString("")
    System.err.println()
    System.err.println(line)
    System.err.println("  $text")
    System.err.println(line)
    System.err.println()
}

fun eprint(s: String) = System.err.println(s)
fun eprint() = System.err.println("")


class Cli : NoOpCliktCommand(
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


class Local : CliktCommand(help = "Build, publish to $m2str") {
    override fun run() = runBlocking {
        cmdLocal()
        Unit
    }
}

open class Signed(help: String = "Build, sign, publish to $m2str") :
    CliktCommand(help = help) {
    val gpgKey by option("--gpg-key", envvar = "MAVEN_GPG_KEY").required()
    val gpgPwd by option("--gpg-password", envvar = "MAVEN_GPG_PASSWORD").required()
    override fun run() = runBlocking {
        cmdSign(
            cmdLocal(),
            key = GpgPrivateKey(gpgKey),
            pass = GpgPassphrase(gpgPwd)
        ).close()
    }
}

open class Stage(help: String = "Build, sign, publish to OSSRH Staging") : Signed(help = help) {
    protected val sonatypeUser by option(
        "--sonatype-username",
        envvar = "SONATYPE_USERNAME").required()
    protected val sonatypePassword by option(
        "--sonatype-password",
        envvar = "SONATYPE_PASSWORD").required()

    override fun run() = runBlocking {
        cmdSign(
            cmdLocal(),
            key = GpgPrivateKey(gpgKey),
            pass = GpgPassphrase(gpgPwd)
        ).use { signed ->
            cmdToStaging(
                signed,
                user = SonatypeUsername(sonatypeUser),
                pass = SonatypePassword(sonatypePassword))
        }
        Unit
    }
}

class Central : Stage(help = "Build, sign, publish to OSSRH Staging, release to Central") {
    override fun run() = runBlocking {
        cmdSign(
            cmdLocal(),
            key = GpgPrivateKey(gpgKey),
            pass = GpgPassphrase(gpgPwd)
        ).use { signed ->
            cmdToStaging(
                signed,
                user = SonatypeUsername(sonatypeUser),
                pass = SonatypePassword(sonatypePassword)).let { (client, uri) ->
                client.promoteToCentral(uri)
            }
        }
    }
}

fun main(args: Array<String>) {
    Cli()
        .subcommands(Local(), Signed(), Stage(), Central())
        .main(args)
}