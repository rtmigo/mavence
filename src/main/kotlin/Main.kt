/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import maven.*
import stages.build.*
import stages.sign.*
import stages.upload.*
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
                "MIT (c) Artsiom iG <ortemeo@gmail.com>\n" +
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

open class Local(help: String = "Build, publish to $m2str") : CliktCommand(help = help) {
    override fun run() = catchingCommand(this) {
        cmdLocal(gaa(), isFinal = true)
        Unit
    }
}

open class Stage(help: String = "Build, sign, publish to OSSRH Staging") :
    Local(help = help) {
    val gpgKey by option("--gpg-key", envvar = "MAVEN_GPG_KEY").required()
    val gpgPwd by option("--gpg-password", envvar = "MAVEN_GPG_PASSWORD").required()

    protected val sonatypeUser by option(
        "--sonatype-username",
        envvar = "SONATYPE_USERNAME").required()
    protected val sonatypePassword by option(
        "--sonatype-password",
        envvar = "SONATYPE_PASSWORD").required()

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
        .subcommands(Local(), Stage(), Central())
        .main(args)
}