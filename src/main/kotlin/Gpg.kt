import com.github.pgreze.process.*
import java.nio.file.Path
import kotlin.io.path.*


@JvmInline
value class GpgPassphrase(val string: String)

@JvmInline
value class GpgPrivateKey(val string: String)

class Gpg {
    val exe: String = "gpg"
}

private suspend fun canGetTty(): Boolean =
    process("tty").resultCode == 0

private fun isGpgTtySet() =
    (System.getenv("GPG_TTY") ?: "").trim().isNotEmpty()

private fun weAreInWindows() =
    System.getProperty("os.name").startsWith("Windows")

suspend fun requireGpgTtyIfNeeded() {
    // есть проблема, которая мешает GPG подписывать файлы в Github Actions:
    // "gpg: signing failed: Inappropriate ioctl for device"
    //
    // у неё есть workaround:
    // https://github.com/keybase/keybase-issues/issues/2798#issue-205008630
    //
    // Это описано также в `man gpg-agent`:
    //
    // You should always add the following lines to your .bashrc  or  whatever
    // initialization file is used for all shell invocations:
    // ```
    //   GPG_TTY=$(tty)
    //   export GPG_TTY
    // ```
    //
    // It is important that this environment variable always reflects the out-
    // put of the tty command.  For W32 systems this option is not required.
    //
    // Я пытался сделать это прямо из скрипта, но в Actions не мог выяснить tty:
    //   1. subprocess.check_output("tty")
    //   2. os.ttyname(sys.stdout.fileno())
    // В первом случае получал код ошибки, во втором
    // "OSError: [Errno 25] Inappropriate ioctl for device"
    //
    // В общем, просто требуем, чтобы переменная была определена
    // Поэтому пока я просто требую, чтобы такая переменная среды была задана
    // до запуска скрипта.

    if (!canGetTty() && !weAreInWindows() && !canGetTty())
        throw Exception(
            """Please set GPG_TTY environment variable: 
               | `export GPG_TTY=${'$'}(tty)`""".trimIndent())
}


/// Creates `file.ext.asc` next to `file.ext`.
suspend fun Gpg.signFile(file: Path, passphrase: GpgPassphrase): Path {
    // 2022-10 я не выяснил, какой именно ключ будет использоваться для подписи.
    // Но в CI он точно один

    requireGpgTtyIfNeeded()

    val ascFile = file.parent.resolve(file.name + ".asc")
    check(!ascFile.exists())
    process(
        this.exe, "--armor", "--detach-sign",
        // танцы с бубном, чтобы gpg не спрашивал пароль интерактивно
        "--batch", "--pinentry-mode", "loopback",
        "--yes", "--passphrase-fd", "0",
        stdin = InputSource.fromString(passphrase.string))
        .also {
            check(it.resultCode == 0)
        }
    check(ascFile.exists())
    return ascFile
}

/// Very dirty function. Imports a new key into the system GPG keys repository,
/// so we can sign files later.
suspend fun Gpg.importKeyToSystem(privateKey: GpgPrivateKey) {
    process(
        this.exe, "--batch", "--import",
        stdin = InputSource.fromString(privateKey.string))
        .also {
            check(it.resultCode == 0)
        }

}
