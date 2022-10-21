/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

import java.nio.file.Path
import kotlin.io.path.*

@JvmInline
value class ProjectRootDir(val path: Path) {
    init {
        assert(path.resolve("gradlew").exists())
    }
}

@JvmInline
value class ArtifactDir(val path: Path) {
    init {
        require(path.isAbsolute)
        require(
            path.resolve("build.gradle.kts").exists() ||
                path.resolve("build.gradle").exists()) {
            "build.gradle not found in $path"
        }
    }
}

@JvmInline
value class BuildGradleFile(val path: Path) {
    init {
        require(path.name.startsWith("build.gradle"))
    }
}

@JvmInline
value class GradlewFile(val path: Path) {
    init {
        require(path.name == "gradlew" || path.name == "gradlew.bat") {
            "Unexpected filename"
        }
        if (path.name == "gradlew")
            check(path.isExecutable()) { "gradlew is not executable" }
    }
}