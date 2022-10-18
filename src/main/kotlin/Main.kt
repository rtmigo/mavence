import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.nio.file.*

class Database : NoOpCliktCommand(
    name="rtmaven",
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

class Local : CliktCommand(help = "Build, publish to $m2str") {
    override fun run() {
        echo("Initialized the database.")
    }
}

open class Signed(help: String = "Build, sign, publish to $m2str") :
    CliktCommand(help = help) {
    private val gpgKey by option("--gpg-key", envvar = "MAVEN_GPG_KEY").required()
    private val gpgPwd by option("--gpg-password", envvar = "MAVEN_GPG_PASSWORD").required()
    override fun run() {
        echo("gpgKey: $gpgKey")
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
//    val parser = ArgParser("rtmaven.jar")
//    val command by parser.argument(
//        ArgType.Choice(Command.values().toList(), { Command.valueOf(it) })
//    )
    //parser.parse(args)

}