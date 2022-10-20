/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package stages.sign

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

/**
 * Runs GPG with a temporary "homedir", i.e. with a temporary keys database.
 * We can import keys into it without changing the system
 */
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
        return process(
            "gpg", "--help",
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
            this.exe, "--batch", "--import",
            stdin = InputSource.fromString(privateKey.string),
            env = this.envWithHome,
        ).unwrap()
    }

    suspend fun verifyFile(
        file: Path,
        signature: Path,
    ): String {
        val prefix = "Primary key fingerprint: "
        return process(
            this.exe, "--verify", signature.toString(), file.toString(),
            env = this.envWithHome, stderr = Redirect.CAPTURE
        ).unwrap()
            .single { it.startsWith(prefix) }.removePrefix(prefix).replace(
                Regex("\\s+"), " ")
    }

    suspend fun signFile(
        file: Path,
        passphrase: GpgPassphrase,
        target: Path = file.parent.resolve(file.name + ".asc"),
    ) {
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
            file.toString(),
            // конец аргументов
            env = this.envWithHome,

            //directory = file.parent.toFile(),
            stdin = InputSource.fromString(passphrase.string))
            .unwrap()
        check(target.exists())
    }
}