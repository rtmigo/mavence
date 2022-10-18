package tools

import com.github.pgreze.process.*
import java.io.*
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.*

@RequiresOptIn
annotation class GpgInternals

@JvmInline
value class GpgPassphrase(val string: String)

@JvmInline
value class GpgPrivateKey(val string: String)

@OptIn(GpgInternals::class)
suspend fun signFile(file: Path,
                     key: GpgPrivateKey,
                     phrase: GpgPassphrase,
                     target: Path = file.parent.resolve(file.name+".asc")) {
    TempGpg().use {
        it.importKey(key)
        it.signFile(file, phrase, target)
    }
}

/// Runs GPG with a temporary "homedir", i.e. with a temporary keys database.
/// We can import keys into it without changing the system

internal class TempGpg : Closeable {
    private val exe: String = "gpg"

    val tempHome: Path = createTempHome()
    private val envWithHome = mapOf("GNUPGHOME" to this.tempHome.toString())

    private var wasKeyImported = false

    override fun close() {
        require(tempHome.toString().contains("tmp"))
        tempHome.toFile().deleteRecursively()
    }

    companion object {
        internal fun createTempHome(): Path {
            // важно оставить право чтения/записи только себе, иначе будет
            // "gpg: WARNING: unsafe permissions on homedir"
            // https://gist.github.com/oseme-techguy/bae2e309c084d93b75a9b25f49718f85
            return createTempDirectory(
                "gpg_home_tmp",
                PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rwx------")
                )
            )
        }
    }

    suspend fun getHome(): Path {
        // the --help command, among other things, prints the home dir used by gpg
        val prefix = "Home: "
        return process("gpg", "--help",
                        stdout = Redirect.CAPTURE,
                        env = envWithHome).unwrap()
            .single { it.startsWith(prefix) }
            .removePrefix(prefix)
            .let { Path(it) }
    }

    suspend fun importKey(privateKey: GpgPrivateKey) {
        check(!this.wasKeyImported) { "key already was imported" }
        this.wasKeyImported = true
        require(this.getHome() == tempHome) {
            "GPG does not seem the respect GNUPGHOME"
        }
        process(
            this.exe,  "--batch", "--import",
            stdin = InputSource.fromString(privateKey.string),
            env = this.envWithHome,
        ).unwrap()
    }

    /// Creates `file.ext.asc` next to `file.ext`.
    suspend fun signFile(file: Path,
                         passphrase: GpgPassphrase,
                         target: Path = file.parent.resolve(file.name+".asc")) {
        // 2022-10 я не выяснил, какой именно ключ будет использоваться для подписи.
        // Но если мы импортировали всего один, то он и будет
        check(this.wasKeyImported) { "key was not imported" }
        check(!target.exists())
        process(
            this.exe, "--armor", "--detach-sign",
            "--output", target.toString(),
            // танцы с бубном, чтобы gpg не спрашивал пароль интерактивно
            "--batch", "--yes", "--no-tty",
            // в старых версиях gpg вместо pinentry-mode был другой аргумент https://bit.ly/3Tz4gpv
            "--pinentry-mode", "loopback",
            "--passphrase-fd", "0",
            env = this.envWithHome,
            directory = file.parent.toFile(),
            stdin = InputSource.fromString(passphrase.string)).unwrap()
        check(target.exists())
    }
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
    //
    // !!! впрочем, возможно задачу решит аргумент "--no-tty". Его использует Gradle
    // https://bit.ly/3Sb4iml

    if (!canGetTty() && !weAreInWindows() && !canGetTty())
        throw Exception(
            """Please set GPG_TTY environment variable: 
               | `export GPG_TTY=${'$'}(tty)`""".trimIndent())
}
