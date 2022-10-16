import java.net.URL
import java.nio.file.Path

@JvmInline
value class Artifact(val string: String)

@JvmInline
value class Group(val string: String)

@JvmInline
value class Version(val string: String)

@JvmInline
value class JarFile(val path: Path)

@JvmInline
value class PomFile(val path: Path)

@JvmInline
value class SignatureFile(val path: Path)

@JvmInline
value class MavenUrl(val url: URL)

@JvmInline
value class ProjectRootDir(val path: Path)

@JvmInline
value class BuildGradleDir(val path: Path)

@JvmInline
value class GradlewFile(val path: Path)

data class Notation(
    val artifact: Artifact,
    val group: Group,
    val version: Version)

// Artifact = NewType('Artifact', str)
//Group = NewType('Group', str)
//Version = NewType('Version', str)
//
//JarFile = NewType('JarFile', Path)
//PomFile = NewType('PomFile', Path)
//SignatureFile = NewType('SignatureFile', Path)
//
//MavenUrl = NewType('MavenUrl', str)
//StagingMavenUrl = NewType('StagingMavenUrl', MavenUrl)
//
//ProjectRootDir = NewType('ProjectRootDir', Path)
//
//BuildGradleDir = NewType('BuildGradleDir', Path)
//"""directory containing build.gradle or build.gradle.kts file"""
//
//BuildGradleFile = NewType('BuildGradleFile', Path)
//"""build.gradle or build.gradle.kts file"""
//
//GradlewFile = NewType('GradlewFile', Path)