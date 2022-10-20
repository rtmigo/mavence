# [mavence](https://github.com/rtmigo/mavence) # experimental

CLI utility for publishing Gradle projects (Kotlin, Java, etc.) to Maven
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

It's very easy to make a Gradle script big and ugly. Especially if
it's supposed to work in CI/CD. Gradle itself is a monster of complexity.
Feeding the monster with excessive tasks is the last thing to do.

However, we still use some Gradle plugins. This is the Gradle `maven-publish`,
that creates, i.e. builds a local copy of a Maven package.

</details>

## Setting the environment

Before publishing, you will need to set the following four environment
variables:

| variable             | wtf                                                       |
|----------------------|-----------------------------------------------------------|
| `SONATYPE_USERNAME`  | Username for Sonatype JIRA (optionally replaced by token) |
| `SONATYPE_PASSWORD`  | Password for Sonatype JIRA (optionally replaced by token) |
| `MAVEN_GPG_KEY`      | Locally generated private key in ASCII armor              |  
| `MAVEN_GPG_PASSWORD` | Password protecting the private key                       |

<details><summary>Here how to get them</summary>

1. You need to register on the Sonatype site and chat with bots in
   their JIRA system, until they **verify** that you can publish a package. That
   gives you `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` you can use for
   publishing.

2. You generate GPG keys in your own terminal. At that point, they are just
   files. It remains to figure out what are **public**, **private** keys and
   what is a **password**. The public key must be sent to a keyserver, and the
   private and password must be exported to variables `MAVEN_GPG_KEY`
   and `MAVEN_GPG_PASSWORD`.

I can't go into more detail as releasing to Maven Central
should be your own hero's journey into the unknown and chilling.
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

java {
    withSourcesJar()
    withJavadocJar()
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
                description.set("There are things even dumber than copy-pasting")
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

## Keep in mind

- Sonatype servers may not respond. If you see a lot of timeout errors,
  it is ok. Postpone work until tomorrow or learn Rust

- If the Sonatype server responds, it does not mean that it is working

- If sending a package to Staging fails for any reason, consider that the
  problem is on your end. Meditate more and try to edit meta-data

## License

Copyright © 2022 [Artsiom iG](https://github.com/rtmigo).
Released under the [ISC License](LICENSE).