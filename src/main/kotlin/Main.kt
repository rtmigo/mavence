
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.nio.file.*

class Database: NoOpCliktCommand() {
    val explicitOpt by option("-e", "--explicit", envvar="z")
    init {
        versionOption(Build.version) {
            "rtmaven $it (${Build.date})\n" +
                "MIT (c) Artsiom iG\n" +
                "http://githib.com/rtmigo/rtmaven_kt" }
    }
    override fun run() = Unit
}

class Init: CliktCommand(help="Initialize the database") {
    override fun run() {
        echo("Initialized the database.")
    }
}

class Drop: CliktCommand(help="Drop the database") {
    override fun run() {
        echo("Dropped the database.")
    }
}

suspend fun prepare(dir: Path = Paths.get(".")) {
    dir.toGradlew().publishAndDetect("central")
}

suspend fun main(args: Array<String>) {
    Database()
        .subcommands(Init(), Drop())
        .main(args)
//    val parser = ArgParser("rtmaven.jar")
//    val command by parser.argument(
//        ArgType.Choice(Command.values().toList(), { Command.valueOf(it) })
//    )
    //parser.parse(args)

}