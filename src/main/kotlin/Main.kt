import kotlinx.cli.*
import java.nio.file.*

private enum class Command {
    prepare, send
}

suspend fun prepare(dir: Path = Paths.get(".")) {
    dir.toGradlew().publishAndDetect("central")
}

suspend fun main(args: Array<String>) {
    val parser = ArgParser("rtmaven.jar")
    val command by parser.argument(
        ArgType.Choice(Command.values().toList(), { Command.valueOf(it) })
    )
    //parser.parse(args)

}