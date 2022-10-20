package stages.sign

import com.github.pgreze.process.process

private suspend fun canGetTty(): Boolean =
    process("tty").resultCode == 0

private fun isGpgTtySet() =
    (System.getenv("GPG_TTY") ?: "").trim().isNotEmpty()

private fun weAreInWindows() =
    System.getProperty("os.name").startsWith("Windows")

@Deprecated("Not needed anymore?", ReplaceWith("")) // since 2022-10
suspend fun requireGpgTtyIfNeeded() {
    // Есть проблема, которая мешает GPG подписывать файлы в Github Actions:
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
    // В общем, эта функция просто проверяла, чтобы переменная была определена
    // до запуска программы.
    //
    // Однако, в коде Gradle https://bit.ly/3Sb4iml нашлось другое решение: аргумент "--no-tty".
    // Похоже, он исправляет проблему и без проверок.
    if (!isGpgTtySet() && !weAreInWindows() && !canGetTty())
        throw Exception(
            """Please set GPG_TTY environment variable: 
               | `export GPG_TTY=${'$'}(tty)`""".trimIndent())
}