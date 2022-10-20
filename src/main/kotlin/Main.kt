/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import maven.*
import stages.sign.cmdSign
import stages.upload.*
import tools.*

import java.nio.file.*




class Cli : NoOpCliktCommand(
    name = "mavence",
    help = "Publishes Gradle projects to Maven Central\n\nSee: https://github.com/rtmigo/mavence#readme"
) {
    init {
        versionOption(Build.version) {
            "$commandName $it (${Build.date})\n" +
                "MIT (c) Artsiom iG <ortemeo@gmail.com>\n" +
                "http://github.com/rtmigo/mavence"
        }
    }

    override fun run() = Unit
}


open class Local(help: String="Build, publish to $m2str") : CliktCommand(help = help) {
    val groupAndArtifact by argument("<artifact>")

    override fun run() = runBlocking {
        cmdLocal(GroupArtifact.parse(groupAndArtifact), isFinal = true)
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

    override fun run() = runBlocking {
        cmdSign(
            cmdLocal(GroupArtifact.parse(this@Stage.groupAndArtifact)),
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
    override fun run() = runBlocking {
        cmdSign(
            cmdLocal(GroupArtifact.parse(groupAndArtifact)),
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

fun main(args: Array<String>) {
    Cli()
        .subcommands(Local(), Stage(), Central())
        .main(args)
}