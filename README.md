# [mavence](https://github.com/rtmigo/mavence) # experimental

CLI utility for publishing Gradle projects (Kotlin, Java, e.t.c) to Maven
Central.

This essentially does the same thing as
the [Signing](https://docs.gradle.org/current/userguide/signing_plugin.html) and
[Nexus](https://github.com/gradle-nexus/publish-plugin) plugins.

<details><summary>Why not use plugins?</summary>

- Building locally
- Publishing somewhere

These tasks are almost unrelated.

By placing publishing logic in a build script, you make the foundation of the
project shaky.

The build script gets big and ugly, especially if it's supposed to work in
CI/CD. Gradle itself is a monster of complexity. Feeding the monster with
excessive tasks is the last thing to do.

However, we still use one of the plugins. This is a Gradle plugin that creates a
local copy of a Maven project.

</details>

## Minimal configuration

We still use Gradle as a base as it makes the project compatible.

```kotlin
// build.gradle.kts

plugins {
    java
    id("java-library")
    id("maven-publish")
}

group = "io.github.doe"
version = "0.1.2"

publishing {
    publications {
        create<MavenPublication>("mylib") {
            from(components["java"])
            pom {
                val repo = "mylib"
                val owner = "doe"

                name.set("mylib")
                description.set("Project Description")
                url.set("https://github.com/$owner/$repo")

                developers {
                    developer {
                        name.set("John Doe")
                        email.set("doe@sample.com")
                    }
                }
                scm {
                    connection.set("scm:git://github.com/$owner/$repo.git")
                    url.set("https://github.com/$owner/$repo")
                }
                licenses {
                    license {
                        name.set("Apache 2.0 License")
                        url.set("https://github.com/$owner/$repo/blob/HEAD/LICENSE")
                    }
                }
            }
        }
    }
}
```

```kotlin
// settings.gradle.kts
rootProject.name = "mylib"
```

## Running

### Publish to Maven Central

Set environment variables `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSWORD`
, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` and run:

```bash
cd /path/to/mylib
java -jar mavence.jar central io.github.doe:mylib 
```

This single command will do all the necessary work: build, signing, staging
and release.

### Publish to Maven Local

This will place the package file inside the local `~/.m2` directory. This way
you can
test the package without sending it anywhere.

```bash
cd /path/to/mylib
java -jar mavence.jar local io.github.doe:mylib 
```

### Publish to Staging

This will push the package to
a [temporary remote repository](https://s01.oss.sonatype.org/content/repositories/)
. This way you can
test the package without sending it to Central.

Set environment variables `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSWORD`
, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` and run:

```bash
cd /path/to/mylib
java -jar mavence.jar stage io.github.doe:mylib 
```

# Keep in mind

- Sonatype servers may not respond. If you see a lot of timeout errors, 
  it is ok, just postpone work until tomorrow or learn Rust

- If the Sonatype server responds, it does not mean that it is working

- If sending a package to Staging fails, consider that the problem is on 
  your end. Try to be a better balanced person and edit the meta data

## License

Copyright © 2022 [Artsiom iG](https://github.com/rtmigo).
Released under the [ISC License](LICENSE).