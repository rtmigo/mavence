/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import maven.*
import stages.build.*
import stages.sign.*
import stages.upload.*
import tools.rethrowingState
import java.nio.file.*
import kotlin.io.path.absolute


class Cli : NoOpCliktCommand(
    name = "mavence",
    help = "Publishes Gradle projects to Maven Central\n\nSee: https://github.com/rtmigo/mavence#readme"
) {
    private val trace by option("--trace", help = "Show full stack traces on errors").flag()

    init {
        versionOption(Build.version) {
            "$commandName $it (${Build.date})\n" +
                "ISC (c) Artsiom iG <ortemeo@gmail.com>\n" +
                "http://github.com/rtmigo/mavence"
        }
    }

    override fun run() {
        currentContext.findOrSetObject { CliConfig(trace = this.trace) }
    }
}

private suspend fun gaa(): GroupArtifact {
    val ad = ArtifactDir(Paths.get(".").absolute())
    return GroupArtifact(ad.group(), ad.artifact())
}

data class CliConfig(val trace: Boolean)

open class CheckCentral : CliktCommand(
    help = "Check that environment is set for publishing to Maven Central") {
    override fun run() = catchingCommand(this) {
        fun checkVar(k: String) = rethrowingState(
            { "Environment variable $k is not set." },
            { check(System.getenv(k).isNotBlank()) })
        checkVar(EnvVarNames.MAVEN_GPG_KEY)
        checkVar(EnvVarNames.MAVEN_GPG_PASSWORD)
        checkVar(EnvVarNames.SONATYPE_USERNAME)
        checkVar(EnvVarNames.SONATYPE_PASSWORD)
        System.err.println("At first glance, everything is OK.")
    }
}

open class Local(help: String = "Build, publish to $m2str") : CliktCommand(help = help) {
    override fun run() = catchingCommand(this) {
        cmdLocal(gaa(), isFinal = true)
        Unit
    }
}

object EnvVarNames {
    const val MAVEN_GPG_KEY = "MAVEN_GPG_KEY"
    const val MAVEN_GPG_PASSWORD = "MAVEN_GPG_PASSWORD"
    const val SONATYPE_USERNAME = "SONATYPE_USERNAME"
    const val SONATYPE_PASSWORD = "SONATYPE_PASSWORD"
}



open class Stage(help: String = "Build, sign, publish to OSSRH Staging") :
    Local(help = help) {
    val gpgKey by option("--gpg-key", envvar = EnvVarNames.MAVEN_GPG_KEY).required()
    val gpgPwd by option("--gpg-password", envvar = EnvVarNames.MAVEN_GPG_PASSWORD).required()

    protected val sonatypeUser by option(
        "--sonatype-username",
        envvar = EnvVarNames.SONATYPE_USERNAME).required()
    protected val sonatypePassword by option(
        "--sonatype-password",
        envvar = EnvVarNames.SONATYPE_PASSWORD).required()

    override fun run() = catchingCommand(this) {
        cmdSign(
            cmdLocal(gaa()),
            key = GpgPrivateKey(gpgKey),
            pass = GpgPassphrase(gpgPwd)
        ).use {
            it.toStaging(
                user = SonatypeUsername(sonatypeUser),
                pass = SonatypePassword(sonatypePassword),
                it.content.notation)
        }
        Unit
    }
}

class Central : Stage(help = "Build, sign, publish to OSSRH Staging, release to Central") {
    override fun run() = catchingCommand(this) {
        cmdSign(
            cmdLocal(gaa()),
            key = GpgPrivateKey(gpgKey),
            pass = GpgPassphrase(gpgPwd)
        ).use { signed ->
            val user = SonatypeUsername(sonatypeUser)
            val pass = SonatypePassword(sonatypePassword)
            signed.toStaging(
                user = user,
                pass = pass,
                signed.content.notation).toRelease(user = user, pass = pass)
        }
    }
}

fun catchingCommand(cmd: CliktCommand, block: suspend () -> Unit) {
    try {
        runBlocking {
            block()
        }
    } catch (e: Exception) {
        if (cmd.currentContext.findObject<CliConfig>()!!.trace)
            e.printStackTrace()
        else
            System.err.println("ERROR: $e")
        System.err.println("Run with --trace to see full stack trace.")
    }

}

fun main(args: Array<String>) {
    Cli()
        .subcommands(Local(), Stage(), Central(), CheckCentral())
        .main(args)
}