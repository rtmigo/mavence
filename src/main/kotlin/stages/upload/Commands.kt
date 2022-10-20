/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package stages.upload

import maven.Notation
import stages.sign.MavenArtifactWithTempSignatures
import tools.*



suspend fun MavenArtifactWithTempSignatures.toStaging(
    user: SonatypeUsername,
    pass: SonatypePassword,
    notation: Notation,
): StagingUri {
    eprintHeader("Creating JAR of JARs")
    return SignedBundle.fromFiles(this).use { bundle ->
        createClient(user, pass).use {
            it.sendToStaging(bundle.jar, notation)
        }
    }
}

suspend fun StagingUri.toRelease(
    user: SonatypeUsername,
    pass: SonatypePassword,
) {
    createClient(user, pass).use {
        it.promoteToCentral(this)
        eprint("HOORAY! We have released the package in Maven Central!")
        eprint("The release may take some time.")
    }
}