import kotlinx.cli.*

private enum class Command {
    prepare, send
}

fun main(args: Array<String>) {
    val parser = ArgParser("rtmaven.jar")
    val command by parser.argument(
        ArgType.Choice(Command.values().toList(), { Command.valueOf(it) })
    )
    parser.parse(args)
}