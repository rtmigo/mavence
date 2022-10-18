import com.github.pgreze.process.*
import java.io.*
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.*


@JvmInline
value class GpgPassphrase(val string: String)

@JvmInline
value class GpgPrivateKey(val string: String)

/// Runs GPG with a temporary "homedir", i.e. with a temporary keys database.
/// We can import keys into it without changing the system
class TempGpg : Closeable {
    val exe: String = "gpg"

    val tempHome: Path = createTempHome()
    val envWithHome = mapOf("GNUPGHOME" to this.tempHome.toString())

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
        println("A")
        require(this.getHome() == tempHome) {
            "GPG does not seem the respect GNUPGHOME"
        }
        println("B")
        process(
            this.exe,  "--batch", "--import",
            stdin = InputSource.fromString(privateKey.string),
//            InputSource.FromStream { out: OutputStream ->
//                out.write("hello world\n".toByteArray())
//                out.flush()
//            },

            //InputSource.fromString("EOF"),
            env = this.envWithHome,
        ).also { println(it) }
            .unwrap()
    }

    /// Creates `file.ext.asc` next to `file.ext`.
    suspend fun signFile(file: Path,
                         passphrase: GpgPassphrase,
                         target: Path = file.parent.resolve(file.name+".asc")) {
        // 2022-10 я не выяснил, какой именно ключ будет использоваться для подписи.
        // Но в CI он точно один

        //requireGpgTtyIfNeeded()

        //val target = file.parent.resolve(file.name + ".asc")
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
            stdin = InputSource.fromString(passphrase.string)).unwrap().also { println(it) }
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
    // !!! впрочем, возможно спасёт аргумент --mo-tty

    if (!canGetTty() && !weAreInWindows() && !canGetTty())
        throw Exception(
            """Please set GPG_TTY environment variable: 
               | `export GPG_TTY=${'$'}(tty)`""".trimIndent())
    // https://github.com/gradle/gradle/blob/5ec3f672ed600a86280be490395d70b7bc634862/subprojects/security/src/main/java/org/gradle/security/internal/gnupg/GnupgSignatory.java
}


//     // GNUPGHOME=/tmp/gpgh
//}


//private fun whoami() = System.getProperty("user.name")


